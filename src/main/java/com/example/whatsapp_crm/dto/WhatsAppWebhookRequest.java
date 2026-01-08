package com.example.whatsapp_crm.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsAppWebhookRequest {
    private String object;
    private List<Entry> entry;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry {
        private String id;
        private List<Change> changes;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Change {
            private Value value;
            private String field;

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Value {
                @JsonProperty("messaging_product")
                private String messagingProduct;
                private Metadata metadata;
                private List<Status> statuses;
                private List<Message> messages;
                private List<Contact> contacts;

                @Data
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class Metadata {
                    @JsonProperty("display_phone_number")
                    private String displayPhoneNumber;

                    @JsonProperty("phone_number_id")
                    private String phoneNumberId;
                }

                @Data
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class Status {
                    private String id;
                    private String status;
                    private String timestamp;
                    @JsonProperty("recipient_id")
                    private String recipientId;
                }

                @Data
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class Message {
                    @JsonProperty("from")
                    private String from;
                    private String id;
                    private String timestamp;
                    @JsonProperty("type")
                    private String type;
                    private Text text;
                    private Image image;
                    private Document document;

                    @Data
                    @JsonIgnoreProperties(ignoreUnknown = true)
                    public static class Text {
                        @JsonProperty("body")
                        private String body;
                    }

                    @Data
                    @JsonIgnoreProperties(ignoreUnknown = true)
                    public static class Image {
                        private String caption;
                        @JsonProperty("mime_type")
                        private String mimeType;
                        private String id;
                    }

                    @Data
                    @JsonIgnoreProperties(ignoreUnknown = true)
                    public static class Document {
                        private String caption;
                        private String filename;
                        @JsonProperty("mime_type")
                        private String mimeType;
                        private String id;
                    }
                }

                @Data
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class Contact {
                    private Profile profile;
                    @JsonProperty("wa_id")
                    private String waId;

                    @Data
                    @JsonIgnoreProperties(ignoreUnknown = true)
                    public static class Profile {
                        private String name;
                    }
                }
            }
        }
    }
}

