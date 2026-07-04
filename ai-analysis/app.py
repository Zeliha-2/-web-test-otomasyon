from __future__ import annotations



import base64

import json

import os

import re

from datetime import datetime

from pathlib import Path



import joblib

import numpy as np

import pandas as pd

import requests

from flask import Flask, jsonify, request



BASE_DIR = Path(__file__).resolve().parent

PROJECT_ROOT = BASE_DIR.parent

MODEL_PATH = BASE_DIR / "qa_model.pkl"

ENCODERS_PATH = BASE_DIR / "qa_encoders.pkl"

HISTORY_DIR = PROJECT_ROOT / "target" / "test-history"

JIRA_REPORTS_JSON = HISTORY_DIR / "jira-reports.json"



FEATURE_COLUMNS = [

    "module",

    "technique",

    "standard",

    "testType",

    "testLevel",

    "selfHealUsed",

    "duration",

]



app = Flask(__name__)

model = None

encoders = None





@app.after_request

def add_cors_headers(response):

    response.headers["Access-Control-Allow-Origin"] = "*"

    response.headers["Access-Control-Allow-Headers"] = "Content-Type"

    response.headers["Access-Control-Allow-Methods"] = "GET, POST, OPTIONS"

    return response





def load_artifacts() -> None:

    global model, encoders

    if not MODEL_PATH.is_file():

        raise FileNotFoundError(f"Model file not found: {MODEL_PATH}")

    if not ENCODERS_PATH.is_file():

        raise FileNotFoundError(f"Encoders file not found: {ENCODERS_PATH}")

    model = joblib.load(MODEL_PATH)

    encoders = joblib.load(ENCODERS_PATH)





def parse_duration(value) -> float:

    if value is None:

        return 0.0

    if isinstance(value, (int, float)):

        return float(value)

    text = str(value).strip().lower().replace("s", "")

    if not text:

        return 0.0

    try:

        return float(text)

    except ValueError:

        return 0.0





def parse_self_heal(value) -> int:

    if isinstance(value, bool):

        return int(value)

    if isinstance(value, (int, float)):

        return 1 if value else 0

    text = str(value).strip().lower()

    return 1 if text in {"1", "true", "yes", "evet"} else 0





def encode_row(payload: dict) -> pd.DataFrame:

    row = {

        "module": str(payload.get("module", "")).strip(),

        "technique": str(payload.get("technique", "")).strip(),

        "standard": str(payload.get("standard", "")).strip(),

        "testType": str(payload.get("testType", "")).strip(),

        "testLevel": str(payload.get("testLevel", "")).strip(),

        "selfHealUsed": parse_self_heal(payload.get("selfHealUsed", False)),

        "duration": parse_duration(payload.get("duration", 0)),

    }



    frame = pd.DataFrame([row])



    for column in ["module", "technique", "standard", "testType", "testLevel"]:

        encoder = encoders.get(column)

        if encoder is None:

            continue

        value = frame.at[0, column]

        if value not in encoder.classes_:

            value = encoder.classes_[0]

        frame[column] = encoder.transform([value])



    return frame[FEATURE_COLUMNS]





def label_name(raw_label) -> str:

    text = str(raw_label).strip().lower()

    if text in {"0", "stable"}:

        return "stable"

    if text in {"1", "flaky"}:

        return "flaky"

    return text





def prediction_probability(prediction_label: str, probabilities: np.ndarray, classes) -> float:

    class_names = [label_name(c) for c in classes]

    if prediction_label in class_names:

        idx = class_names.index(prediction_label)

        return round(float(probabilities[idx]), 4)

    return round(float(np.max(probabilities)), 4)





def jira_settings() -> dict:

    cfg_path = HISTORY_DIR / "jira-config.json"

    file_cfg = {}

    if cfg_path.is_file():

        try:

            file_cfg = json.loads(cfg_path.read_text(encoding="utf-8"))

        except json.JSONDecodeError:

            file_cfg = {}



    base_url = os.getenv("JIRA_BASE_URL") or file_cfg.get("baseUrl") or ""

    email = os.getenv("JIRA_EMAIL") or ""

    token = os.getenv("JIRA_API_TOKEN") or os.getenv("ATLASSIAN_API_TOKEN") or ""

    project_key = os.getenv("JIRA_PROJECT_KEY") or file_cfg.get("projectKey") or "QA"

    enabled = str(os.getenv("JIRA_ENABLED", file_cfg.get("enabled", False))).lower() in {

        "1",

        "true",

        "yes",

    }

    return {

        "enabled": enabled and bool(base_url and email and token),

        "baseUrl": base_url.rstrip("/"),

        "email": email,

        "token": token,

        "projectKey": project_key,

        "issueType": os.getenv("JIRA_ISSUE_TYPE", "Bug"),

    }





def jira_auth_header(email: str, token: str) -> dict:

    raw = f"{email}:{token}".encode("utf-8")

    return {"Authorization": "Basic " + base64.b64encode(raw).decode("ascii")}





def escape_jql(value: str) -> str:

    return value.replace("\\", "\\\\").replace('"', '\\"')





def build_duplicate_jql(case_id: str, summary: str, error_message: str, project_key: str) -> str:

    jql = f'project = {project_key} AND issuetype = Bug AND status != Done'

    if case_id:

        safe = escape_jql(case_id)

        jql += f' AND (summary ~ "{safe}" OR description ~ "{safe}")'

    elif summary:

        token = summary[:40]

        jql += f' AND summary ~ "{escape_jql(token)}"'

    elif error_message and len(error_message) > 20:

        jql += f' AND description ~ "{escape_jql(error_message[:20])}"'

    return jql + " ORDER BY created DESC"





def load_jira_reports() -> list:

    if not JIRA_REPORTS_JSON.is_file():

        return []

    try:

        data = json.loads(JIRA_REPORTS_JSON.read_text(encoding="utf-8"))

        return data if isinstance(data, list) else []

    except json.JSONDecodeError:

        return []





def save_jira_report(entry: dict) -> None:

    reports = load_jira_reports()

    reports.insert(0, entry)

    HISTORY_DIR.mkdir(parents=True, exist_ok=True)

    JIRA_REPORTS_JSON.write_text(json.dumps(reports[:200], indent=2), encoding="utf-8")





def resolve_screenshot(relative_path: str) -> Path | None:

    if not relative_path:

        return None

    candidate = (HISTORY_DIR / relative_path).resolve()

    if candidate.is_file():

        return candidate

    alt = (PROJECT_ROOT / "target" / relative_path.replace("\\", "/")).resolve()

    if alt.is_file():

        return alt

    alt2 = (PROJECT_ROOT / "target" / "screenshots" / Path(relative_path).name).resolve()

    if alt2.is_file():

        return alt2

    return None





def plain_description_from_report(report: dict) -> str:

    parts = [

        f"Hata Tarihi: {report.get('detectedAt', '')}",

        "",

        "Test Adımları:",

        report.get("testSteps", ""),

        "",

        "Beklenen Sonuç:",

        report.get("expectedResult", ""),

        "",

        "Gerçekleşen Sonuç:",

        report.get("actualResult", ""),

        "",

        report.get("description", ""),

    ]

    return "\n".join(parts).strip()





@app.route("/health", methods=["GET"])

def health():

    cfg = jira_settings()

    return jsonify(

        {

            "status": "ok",

            "model_loaded": model is not None,

            "jira_configured": cfg["enabled"],

        }

    )





@app.route("/predict", methods=["POST"])

def predict():

    if model is None or encoders is None:

        return jsonify({"error": "Model artifacts are not loaded."}), 503



    payload = request.get_json(silent=True)

    if not isinstance(payload, dict):

        return jsonify({"error": "Request body must be JSON."}), 400



    missing = [field for field in FEATURE_COLUMNS if field not in payload]

    if missing:

        return jsonify({"error": f"Missing fields: {', '.join(missing)}"}), 400



    try:

        features = encode_row(payload)

        raw_prediction = model.predict(features)[0]

        prediction = label_name(raw_prediction)



        probability = 0.0

        if hasattr(model, "predict_proba"):

            probabilities = model.predict_proba(features)[0]

            probability = prediction_probability(prediction, probabilities, model.classes_)



        return jsonify({"prediction": prediction, "probability": probability})

    except Exception as exc:

        return jsonify({"error": str(exc)}), 500





@app.route("/jira/config", methods=["GET", "OPTIONS"])

def jira_config():

    if request.method == "OPTIONS":

        return ("", 204)

    cfg_path = HISTORY_DIR / "jira-config.json"

    file_cfg = {}

    if cfg_path.is_file():

        try:

            file_cfg = json.loads(cfg_path.read_text(encoding="utf-8"))

        except json.JSONDecodeError:

            file_cfg = {}

    runtime = jira_settings()

    return jsonify(

        {

            "enabled": runtime["enabled"],

            "baseUrl": runtime["baseUrl"] or file_cfg.get("baseUrl", ""),

            "projectKey": runtime["projectKey"],

            "proxyUrl": file_cfg.get("proxyUrl", "http://127.0.0.1:5000"),

            "channel": file_cfg.get("channel", "Web"),

            "qaOrigin": file_cfg.get("qaOrigin", "Test Otomasyon"),

            "qaProcess": file_cfg.get("qaProcess", "Regression"),

            "hasCredentials": runtime["enabled"],

        }

    )





@app.route("/jira/duplicate-check", methods=["POST", "OPTIONS"])

def jira_duplicate_check():

    if request.method == "OPTIONS":

        return ("", 204)



    payload = request.get_json(silent=True) or {}

    case_id = str(payload.get("caseId", "")).strip()

    summary = str(payload.get("summary", "")).strip()

    error_message = str(payload.get("errorMessage", "")).strip()



    local_matches = []

    norm = re.sub(r"\s+", " ", error_message.lower())[:120]

    for item in load_jira_reports():

        if case_id and item.get("caseId") == case_id:

            local_matches.append(item)

            continue

        prev_err = re.sub(r"\s+", " ", str(item.get("errorMessage", "")).lower())[:120]

        if case_id and item.get("caseId") == case_id and prev_err == norm:

            local_matches.append(item)



    cfg = jira_settings()

    remote_matches = []

    if cfg["enabled"]:

        jql = build_duplicate_jql(case_id, summary, error_message, cfg["projectKey"])

        url = f"{cfg['baseUrl']}/rest/api/3/search"

        try:

            resp = requests.get(

                url,

                params={"jql": jql, "maxResults": 5, "fields": "summary,status"},

                headers={**jira_auth_header(cfg["email"], cfg["token"]), "Accept": "application/json"},

                timeout=15,

            )

            if resp.status_code < 300:

                for issue in resp.json().get("issues", []):

                    key = issue.get("key", "")

                    remote_matches.append(

                        {

                            "issueKey": key,

                            "summary": issue.get("fields", {}).get("summary", ""),

                            "status": issue.get("fields", {}).get("status", {}).get("name", ""),

                            "url": f"{cfg['baseUrl']}/browse/{key}",

                            "source": "jira",

                        }

                    )

        except requests.RequestException as exc:

            return jsonify({"error": str(exc), "localMatches": local_matches, "matches": local_matches}), 200



    matches = []

    seen = set()

    for item in local_matches + remote_matches:

        key = item.get("issueKey", "")

        if key and key not in seen:

            seen.add(key)

            matches.append(item)



    return jsonify({"matches": matches, "duplicateFound": len(matches) > 0})





@app.route("/jira/create", methods=["POST", "OPTIONS"])

def jira_create():

    if request.method == "OPTIONS":

        return ("", 204)



    payload = request.get_json(silent=True) or {}

    report = payload.get("report") or {}

    cfg = jira_settings()

    if not cfg["enabled"]:

        return jsonify(

            {

                "success": False,

                "message": "Jira API yapılandırılmadı. Formu kopyalayıp manuel oluşturabilirsiniz.",

                "manualMode": True,

            }

        ), 200



    summary = str(report.get("summary", "Otomasyon Hatası")).strip()

    description = plain_description_from_report(report)

    priority = str(report.get("priority", "Medium"))

    issue_type = str(report.get("issueType") or cfg["issueType"])



    fields = {

        "project": {"key": report.get("projectKey") or cfg["projectKey"]},

        "issuetype": {"name": issue_type},

        "summary": summary,

        "description": {

            "type": "doc",

            "version": 1,

            "content": [

                {

                    "type": "paragraph",

                    "content": [{"type": "text", "text": description[:32000]}],

                }

            ],

        },

    }

    if priority and priority.lower() != "none":

        fields["priority"] = {"name": priority}



    create_url = f"{cfg['baseUrl']}/rest/api/3/issue"

    try:

        resp = requests.post(

            create_url,

            headers={

                **jira_auth_header(cfg["email"], cfg["token"]),

                "Accept": "application/json",

                "Content-Type": "application/json",

            },

            json={"fields": fields},

            timeout=20,

        )

        if resp.status_code >= 300:

            return jsonify({"success": False, "httpStatus": resp.status_code, "message": resp.text}), 200

        issue = resp.json()

        issue_key = issue.get("key", "")

        issue_url = f"{cfg['baseUrl']}/browse/{issue_key}"



        screenshot = resolve_screenshot(str(report.get("screenshotPath", "")))

        attached = False

        if issue_key and screenshot:

            attach_url = f"{cfg['baseUrl']}/rest/api/3/issue/{issue_key}/attachments"

            with screenshot.open("rb") as fh:

                attach_resp = requests.post(

                    attach_url,

                    headers={

                        **jira_auth_header(cfg["email"], cfg["token"]),

                        "X-Atlassian-Token": "no-check",

                    },

                    files={"file": (screenshot.name, fh, "image/png")},

                    timeout=30,

                )

            attached = attach_resp.status_code < 300



        entry = {

            "caseId": report.get("caseId", ""),

            "issueKey": issue_key,

            "url": issue_url,

            "summary": summary,

            "errorMessage": report.get("actualResult", ""),

            "reportedAt": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),

            "screenshotAttached": attached,

        }

        save_jira_report(entry)



        return jsonify(

            {

                "success": True,

                "issueKey": issue_key,

                "url": issue_url,

                "screenshotAttached": attached,

            }

        )

    except requests.RequestException as exc:

        return jsonify({"success": False, "message": str(exc)}), 500





if __name__ == "__main__":

    load_artifacts()

    app.run(host="0.0.0.0", port=5000, debug=True)

else:

    try:

        load_artifacts()

    except FileNotFoundError as exc:

        print(f"[WARN] {exc}")


