import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Класс CrptApi работает с API Честного знака
 * Данный класс поддерживает ограничение на количество запросов к API
 */
public class CrptApi {
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private final String apiUrl;

    /**
     * Создание класса CrptApi с указанием AP URL
     *
     * @param timeUnit - единица времени для интервала запросов
     * @param requestLimit - максимальное количество запросов в заданный интервал времени
     * @param apiUrl - URL API запроса
     */
    public CrptApi(TimeUnit timeUnit, int requestLimit, String apiUrl) {
        this.semaphore = new Semaphore(requestLimit);
        this.apiUrl = apiUrl;

        scheduler = Executors.newSingleThreadScheduledExecutor();
        setSchedule(timeUnit, 1, 1);
    }

    /**
     * Создание класса CrptApi
     * AP URL запроса по умолчанию равен https://ismp.crpt.ru/api/v3/lk/documents/create
     *
     * @param timeUnit - единица времени для интервала запросов
     * @param requestLimit - максимальное количество запросов в заданный интервал времени
     */
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this(timeUnit, requestLimit, "https://ismp.crpt.ru/api/v3/lk/documents/create");
    }


    /**
     * Метод создания документа
     *
     * @param document - документ
     * @param signature - подпись
     */
    public void createDocument(Document document, String signature) {
        try {
            semaphore.acquire();

            HttpRequest request = buildRequest(document, signature);
            HttpResponse<String> response = send(request);

            if (response.statusCode() == 200) {
                // В зависимости от задачи обрабатываем этом. Сейчас просто выводим ответ в консоль.
                System.out.println("Success: " + response.body());
            } else {
                System.out.println("Error: " + response.statusCode());
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
    }


    /**
     * Метод для построения HTTP запроса
     *
     * @param document - документ для создания запроса
     * @param signature - подпись для документа
     * @return HttpRequest - HTTP запрос
     */
    private HttpRequest buildRequest(Document document, String signature) {
        String json = convertToJson(document);
        return HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }

    /**
     * Метод для отправки HTTP запроса
     *
     * @param request - HTTP запрос
     * @return - HttpResponse<String> - ответ
     */
    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }


    /**
     * Сброс лимита
     */
    private void resetRequestLimit() {
        semaphore.release(semaphore.availablePermits());
    }

    /**
     * Задание периодической задачи сброса счетчика запросов
     *
     * @param timeUnit - единица времени для интервала запросов
     * @param initDelay - начальный интервал
     * @param period - периодичность
     */
    public void setSchedule(TimeUnit timeUnit, long initDelay, long period) {
        scheduler.scheduleAtFixedRate(this::resetRequestLimit, timeUnit.toMillis(initDelay), timeUnit.toMillis(period), TimeUnit.MILLISECONDS);
    }

    /**
     * Метод для остановки планировщика.
     */
    public void shutdown() {
        scheduler.shutdown();
    }

    /**
     * Метод для преобразования документа в JSON
     *
     * @param document - документ, который будет преобразоваться в JSON
     * @return JSON
     */
    private String convertToJson(Document document) {
        Gson gson = new Gson();
        return gson.toJson(document);
    }

    /**
     * Класс описывающий документ
     */
    static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;

        public Description getDescription() {
            return description;
        }

        public void setDescription(Description description) {
            this.description = description;
        }

        public String getDoc_id() {
            return doc_id;
        }

        public void setDoc_id(String doc_id) {
            this.doc_id = doc_id;
        }

        public String getDoc_status() {
            return doc_status;
        }

        public void setDoc_status(String doc_status) {
            this.doc_status = doc_status;
        }

        public String getDoc_type() {
            return doc_type;
        }

        public void setDoc_type(String doc_type) {
            this.doc_type = doc_type;
        }

        public boolean isImportRequest() {
            return importRequest;
        }

        public void setImportRequest(boolean importRequest) {
            this.importRequest = importRequest;
        }

        public String getOwner_inn() {
            return owner_inn;
        }

        public void setOwner_inn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        public String getParticipant_inn() {
            return participant_inn;
        }

        public void setParticipant_inn(String participant_inn) {
            this.participant_inn = participant_inn;
        }

        public String getProducer_inn() {
            return producer_inn;
        }

        public void setProducer_inn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        public String getProduction_date() {
            return production_date;
        }

        public void setProduction_date(String production_date) {
            this.production_date = production_date;
        }

        public String getProduction_type() {
            return production_type;
        }

        public void setProduction_type(String production_type) {
            this.production_type = production_type;
        }

        public List<Product> getProducts() {
            return products;
        }

        public void setProducts(List<Product> products) {
            this.products = products;
        }

        public String getReg_date() {
            return reg_date;
        }

        public void setReg_date(String reg_date) {
            this.reg_date = reg_date;
        }

        public String getReg_number() {
            return reg_number;
        }

        public void setReg_number(String reg_number) {
            this.reg_number = reg_number;
        }
    }

    /**
     * Описание
     */
    public class Description {
        private String participantInn;

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }
    }

    /**
     * Продукт
     */
    public class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;

        public String getCertificate_document() {
            return certificate_document;
        }

        public void setCertificate_document(String certificate_document) {
            this.certificate_document = certificate_document;
        }

        public String getCertificate_document_date() {
            return certificate_document_date;
        }

        public void setCertificate_document_date(String certificate_document_date) {
            this.certificate_document_date = certificate_document_date;
        }

        public String getCertificate_document_number() {
            return certificate_document_number;
        }

        public void setCertificate_document_number(String certificate_document_number) {
            this.certificate_document_number = certificate_document_number;
        }

        public String getOwner_inn() {
            return owner_inn;
        }

        public void setOwner_inn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        public String getProducer_inn() {
            return producer_inn;
        }

        public void setProducer_inn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        public String getProduction_date() {
            return production_date;
        }

        public void setProduction_date(String production_date) {
            this.production_date = production_date;
        }

        public String getTnved_code() {
            return tnved_code;
        }

        public void setTnved_code(String tnved_code) {
            this.tnved_code = tnved_code;
        }

        public String getUit_code() {
            return uit_code;
        }

        public void setUit_code(String uit_code) {
            this.uit_code = uit_code;
        }

        public String getUitu_code() {
            return uitu_code;
        }

        public void setUitu_code(String uitu_code) {
            this.uitu_code = uitu_code;
        }
    }

    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 1);

        String json = "";
        try {
            byte[] bytes = Files.readAllBytes(Paths.get("src/main/java/document.json"));
            json = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Gson gson = new Gson();
        CrptApi.Document document = gson.fromJson(json, CrptApi.Document.class);
        api.createDocument(document, "signature");
        api.shutdown();
    }
}