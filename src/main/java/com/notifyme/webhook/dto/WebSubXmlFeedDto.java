package com.notifyme.webhook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * DTO para desserialização do Atom XML Feed enviado pelo YouTube WebSub.
 * 
 * O Jackson XML converte o payload HTTP bruto diretamente para esta estrutura tipada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "feed")
public class WebSubXmlFeedDto implements Serializable {

    @JacksonXmlProperty(localName = "title")
    private String feedTitle;

    @JacksonXmlProperty(localName = "entry")
    private List<FeedEntry> entries;

    /**
     * Representa a tag <entry> que contém os detalhes do vídeo publicado ou atualizado.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FeedEntry implements Serializable {

        @JacksonXmlProperty(localName = "videoId", namespace = "http://www.youtube.com/xml/schemas/2015")
        private String videoId;

        @JacksonXmlProperty(localName = "channelId", namespace = "http://www.youtube.com/xml/schemas/2015")
        private String channelId;

        @JacksonXmlProperty(localName = "title")
        private String title;

        @JacksonXmlProperty(localName = "link")
        private FeedLink link;

        @JacksonXmlProperty(localName = "published")
        private String published;

        @JacksonXmlProperty(localName = "updated")
        private String updated;
    }

    /**
     * Representa a tag <link rel="alternate" href="..."/>
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FeedLink implements Serializable {

        @JacksonXmlProperty(isAttribute = true, localName = "href")
        private String href;

        @JacksonXmlProperty(isAttribute = true, localName = "rel")
        private String rel;
    }
}
