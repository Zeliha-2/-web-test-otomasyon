package com.automation.models; // Bu sınıfın paketini belirler (model katmanı)

public class NewsletterScenario { // JSON'dan okunacak senaryo nesnesi
    private String caseId; // Senaryonun benzersiz ID'si (ör: NWS-001)
    private String description; // Senaryonun insan okunur açıklaması
    private String priority; // Öncelik (P0, P1...)
    private String technique; // ISTQB tekniği (state-transition vb.)
    private String testType;
    private String testLevel;
    private String standard;
    private String reference;
    private Boolean subscribe; // true -> Yes, false -> No seçimi
    private String expectedSuccessContains; // Başarı mesajında beklenen parça

    public String getCaseId() { // caseId alanını dışarıya okumak için getter
        return caseId; // caseId değerini döndürür
    }

    public String getDescription() { // description için getter
        return description; // description döner
    }

    public String getPriority() { // priority için getter
        return priority; // priority döner
    }

    public String getTechnique() { // technique için getter
        return technique; // technique döner
    }

    public String getTestType() {
        return testType;
    }

    public String getTestLevel() {
        return testLevel;
    }

    public String getStandard() {
        return standard;
    }

    public String getReference() {
        return reference;
    }

    public Boolean getSubscribe() { // subscribe (Yes/No) için getter
        return subscribe; // subscribe döner
    }

    public String getExpectedSuccessContains() { // beklenen mesaj parçası getter
        return expectedSuccessContains; // mesaj parçasını döndürür
    }
}