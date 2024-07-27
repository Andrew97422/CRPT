package ru.andrew;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Date;

public class CrptApi {
    private final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final String CLIENT_TOKEN = "clientToken";
    private final String USER_NAME = "userName";

    private int requestLimit;
    private final TimeUnit timeUnit;
    private static int counter;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        counter = requestLimit;
    }

    public void runRequest(Document document) {
        String docJson = getDocJson(document).toString();
        httpRequest(docJson);
    }

    @SuppressWarnings("unchecked")
    private JSONObject getDocJson(Document document) {
        JSONObject doc = new JSONObject();
        if (isNull(document.getDescription().getParticipantInn())) {
            JSONObject inn = new JSONObject();
            inn.put("participantInn", document.getParticipantInn());
            doc.put("description", inn);
        }
        doc.put("doc_id", document.getDocId());
        doc.put("doc_status", document.getDocStatus());
        doc.put("doc_type", document.getDocType());
        if (isNull(document.getImportRequest())) {
            doc.put("importRequest", document.getImportRequest());
        }
        doc.put("owner_inn", document.getOwnerInn());
        doc.put("participant_inn", document.getParticipantInn());
        doc.put("producer_inn", document.getProducerInn());
        doc.put("production_date", document.getProducerInn());
        doc.put("production_type", document.getProductionType());
        Document.Products product = document.getProducts();
        if (product != null) {
            JSONArray productsList = new JSONArray();
            JSONObject products = new JSONObject();

            if (product.getCertificateDocument() != null) {
                products.put("certificate_document", product.getCertificateDocument());
            } else if (isNull(product.getCertificateDocumentDate())) {
                products.put("certificate_document_date", product.getCertificateDocumentDate());
            } else if (isNull(product.getCertificateDocumentNumber())) {
                products.put("certificate_document_number", product.getCertificateDocumentNumber());
            }

            products.put("owner_inn", document.getOwnerInn());
            products.put("producer_inn", document.getProducerInn());
            products.put("production_date", document.getProductionDate());

            if (!document.getProductionDate().equals(product.getProductionDate())) {
                products.put("production_date", product.getProductionDate());
            }
            products.put("tnved_code", product.tnvedCode);

            if (isNull(product.getUitCode())) {
                products.put("uit_code", product.getUitCode());
            } else if (isNull(product.getUituCode())) {
                products.put("uitu_code", product.getUituCode());
            } else {
                throw new IllegalArgumentException("Одно из полей uit_code/uitu_code " +
                        "является обязательным");
            }
            productsList.add(products);
            doc.put("products", productsList);
        }
        doc.put("reg_date", document.getRegDate());
        doc.put("reg_number", document.getRegNumber());
        return doc;
    }

    private void httpRequest(String json) {
        if (requestLimit != 0) {
            synchronized (this) {
                counter--;
            }
        }
        try {
            if (counter < 0) {
                Thread.sleep(getTime());
                counter = requestLimit;
            }
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost(URL);

            StringEntity entity = new StringEntity(json);
            post.addHeader("content-type", "application/json");
            post.addHeader("clientToken", CLIENT_TOKEN);
            post.addHeader("userName", USER_NAME);
            post.setEntity(entity);
            httpClient.execute(post);
            httpClient.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean isNull(String check) {
        return check != null;
    }

    public enum TimeUnit {
        SECOND, MINUTE, HOUR
    }

    private long getTime() {
        return switch (timeUnit) {
            case SECOND -> 1000;
            case MINUTE -> 1000 * 60;
            case HOUR -> 1000 * 60 * 60;
        };
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    public static class Document {

        private Description description;
        private final String docId;
        private final String docStatus;
        private final String docType;
        private String importRequest;
        private final String ownerInn;
        private String participantInn;
        private final String producerInn;
        private final Date productionDate;
        private final String productionType;
        private Products products;
        private final Date regDate;
        private final String regNumber;

        @Getter
        @Setter
        public static class Description {
            private String participantInn;
        }

        @Getter
        @Setter
        public static class Products {
            private String certificateDocument;
            private String certificateDocumentDate;
            private String certificateDocumentNumber;
            private String ownerInn;
            private String producerInn;
            private Date productionDate;
            private String tnvedCode;
            private String uitCode;
            private String uituCode;
        }
    }
}
