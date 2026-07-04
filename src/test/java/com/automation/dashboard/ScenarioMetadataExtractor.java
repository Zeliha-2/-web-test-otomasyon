package com.automation.dashboard;



import com.automation.models.AccountSettingsScenario;

import com.automation.models.ApiScenario;

import com.automation.models.CartScenario;

import com.automation.models.CheckoutScenario;

import com.automation.models.DbScenario;

import com.automation.models.E2EScenario;

import com.automation.models.ForgotPasswordScenario;

import com.automation.models.LoginScenario;

import com.automation.models.MyAccountScenario;

import com.automation.models.NewsletterScenario;

import com.automation.models.RegisterScenario;

import com.automation.models.SearchScenario;

import com.automation.models.WishListScenario;



final class ScenarioMetadataExtractor {

    private ScenarioMetadataExtractor() {

    }



    static void applyScenarioObject(Object param, DashboardTestEntry entry) {

        if (param == null) {

            return;

        }

        if (param instanceof LoginScenario s) {

            copy(s.getCaseId(), s.getDescription(), s.getPriority(), s.getTechnique(), s.getTestType(), s.getTestLevel(), s.getStandard(), s.getReference(), entry);

            put(entry, "email", s.getEmail());

            put(entry, "password", maskSecret(s.getPassword()));

            put(entry, "expectWarning", String.valueOf(s.isExpectWarning()));

            put(entry, "expectedWarningContains", s.getExpectedWarningContains());

        } else if (param instanceof RegisterScenario s) {

            copy(s.getCaseId(), s.getDescription(), s.getPriority(), s.getTechnique(), s.getTestType(), s.getTestLevel(), s.getStandard(), s.getReference(), entry);

        } else if (param instanceof CartScenario s) {

            copy(s.getCaseId(), s.getDescription(), s.getPriority(), s.getTechnique(), s.getTestType(), s.getTestLevel(), s.getStandard(), s.getReference(), entry);

        } else if (param instanceof CheckoutScenario s) {

            copy(s.getCaseId(), s.getDescription(), s.getPriority(), s.getTechnique(), s.getTestType(), s.getTestLevel(), s.getStandard(), s.getReference(), entry);

        } else if (param instanceof ForgotPasswordScenario s) {

            copy(s.getCaseId(), s.getDescription(), s.getPriority(), s.getTechnique(), s.getTestType(), s.getTestLevel(), s.getStandard(), s.getReference(), entry);

            put(entry, "email", s.getEmail());

            put(entry, "expectSuccess", String.valueOf(s.isExpectSuccess()));

            put(entry, "expectedMessageContains", s.getExpectedMessageContains());

        } else if (param instanceof MyAccountScenario s) {

            copy(s.getCaseId(), s.getDescription(), s.getPriority(), s.getTechnique(), s.getTestType(), s.getTestLevel(), s.getStandard(), s.getReference(), entry);

        } else if (param instanceof AccountSettingsScenario s) {

            copy(s.getCaseId(), s.getDescription(), s.getPriority(), s.getTechnique(), s.getTestType(), s.getTestLevel(), s.getStandard(), s.getReference(), entry);

        } else if (param instanceof NewsletterScenario s) {

            copy(s.getCaseId(), s.getDescription(), s.getPriority(), s.getTechnique(), s.getTestType(), s.getTestLevel(), s.getStandard(), s.getReference(), entry);

        } else if (param instanceof SearchScenario s) {

            copy(s.getCaseId(), s.getDescription(), s.getPriority(), s.getTechnique(), s.getTestType(), s.getTestLevel(), s.getStandard(), s.getReference(), entry);

        } else if (param instanceof WishListScenario s) {

            copy(s.getCaseId(), s.getDescription(), s.getPriority(), s.getTechnique(), s.getTestType(), s.getTestLevel(), s.getStandard(), s.getReference(), entry);

        } else if (param instanceof ApiScenario s) {

            copy(s.getCaseId(), s.getDescription(), s.getPriority(), s.getTechnique(), s.getTestType(), s.getTestLevel(), s.getStandard(), s.getReference(), entry);

        } else if (param instanceof DbScenario s) {

            copy(s.getCaseId(), s.getDescription(), s.getPriority(), s.getTechnique(), s.getTestType(), s.getTestLevel(), s.getStandard(), s.getReference(), entry);

        } else if (param instanceof E2EScenario s) {

            copy(s.getCaseId(), s.getDescription(), s.getPriority(), s.getTechnique(), s.getTestType(), s.getTestLevel(), s.getStandard(), s.getReference(), entry);

        }

    }



    private static void put(DashboardTestEntry entry, String key, String value) {

        if (value != null && !value.isBlank()) {

            entry.scenarioContext.put(key, value.trim());

        }

    }



    private static String maskSecret(String value) {

        if (value == null || value.isBlank()) {

            return "";

        }

        if (value.length() <= 2) {

            return "**";

        }

        return value.charAt(0) + "****" + value.charAt(value.length() - 1);

    }



    private static void copy(

            String caseId,

            String description,

            String priority,

            String technique,

            String testType,

            String testLevel,

            String standard,

            String reference,

            DashboardTestEntry entry) {

        if (caseId != null) {

            entry.caseId = caseId;

        }

        if (description != null) {

            entry.description = description;

        }

        if (priority != null) {

            entry.priority = priority;

        }

        if (technique != null) {

            entry.technique = technique;

        }

        if (testType != null) {

            entry.testType = testType;

        }

        if (testLevel != null) {

            entry.testLevel = testLevel;

        }

        if (standard != null) {

            entry.standard = standard;

        }

        if (reference != null) {

            entry.reference = reference;

        }

    }

}


