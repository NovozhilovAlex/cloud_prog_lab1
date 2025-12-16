package com.example.cloud_prog_lab1.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/vision")
@CrossOrigin(origins = "http://localhost:3000")
public class VisionController {

    private final RestTemplate restTemplate;
    private final String vkCloudApiUrl;
    private final String oauthToken;
    private final ObjectMapper objectMapper;

    public VisionController(RestTemplate restTemplate,
                            @Value("${vk.cloud.api.url:https://smarty.mail.ru/api/v1/objects/detect}") String vkCloudApiUrl,
                            @Value("${vk.cloud.oauth.token}") String oauthToken,
                            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.vkCloudApiUrl = vkCloudApiUrl;
        this.oauthToken = oauthToken;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/uploadAndDetect", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> uploadImage(@RequestParam("file") MultipartFile image) throws IOException {
        if (image.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        BufferedImage originalImage = ImageIO.read(image.getInputStream());
        if (originalImage == null) {
            return ResponseEntity.status(400).body(("Could not read image format.").getBytes());
        }
        String originalFileName = image.getOriginalFilename();
        String contentType = image.getContentType();

        String metaJsonString = objectMapper.createObjectNode()
                .putPOJO("mode", new String[]{"scene", "multiobject", "pedestrian"})
                .set("images", objectMapper.createArrayNode()
                        .add(objectMapper.createObjectNode()
                                .put("name", "file")))
                .toString();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        ByteArrayResource fileResource = new ByteArrayResource(image.getBytes()) {
            @Override
            public String getFilename() {
                return originalFileName;
            }
        };
        body.add("file", fileResource);
        body.add("meta", metaJsonString);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        String fullUrl = vkCloudApiUrl + "?oauth_token=" + this.oauthToken + "&oauth_provider=mcs";

        ResponseEntity<String> response = restTemplate.postForEntity(fullUrl, requestEntity, String.class);
        JsonNode cloudResponse = objectMapper.readTree(response.getBody());

        List<BoundingBoxInfo> boxesInfo = new ArrayList<>();
        JsonNode multiobjectLabels = cloudResponse.path("body").path("multiobject_labels");

        if (multiobjectLabels.isArray()) {
            for (JsonNode labelSet : multiobjectLabels) {
                if (labelSet.path("name").asText().equals("file")) {
                    JsonNode labels = labelSet.path("labels");
                    if (labels.isArray()) {
                        for (JsonNode label : labels) {
                            JsonNode coordNode = label.path("coord");
                            JsonNode engLabelNode = label.path("rus");

                            if (coordNode != null && coordNode.isArray() && coordNode.size() == 4 && engLabelNode
                                    != null && !engLabelNode.isNull()) {
                                int[] coord = new int[4];
                                coord[0] = coordNode.get(0).asInt();
                                coord[1] = coordNode.get(1).asInt();
                                coord[2] = coordNode.get(2).asInt();
                                coord[3] = coordNode.get(3).asInt();
                                String engLabel = engLabelNode.asText();
                                boxesInfo.add(new BoundingBoxInfo(coord, engLabel));
                            }
                        }
                    }
                }
            }
        }

        if (!boxesInfo.isEmpty()) {
            BufferedImage imageWithBoxes = drawBoundingBoxAndLabel(originalImage, boxesInfo);

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(imageWithBoxes, "jpeg", baos);
                byte[] resultImageBytes = baos.toByteArray();

                HttpHeaders imageHeaders = new HttpHeaders();
                imageHeaders.setContentType(MediaType.IMAGE_JPEG);
                imageHeaders.setContentLength(resultImageBytes.length);
                return ResponseEntity.ok().headers(imageHeaders).body(resultImageBytes);
            }
        } else {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(originalImage, "jpeg", baos);
                return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(baos.toByteArray());
            }
        }
    }

    private BufferedImage drawBoundingBoxAndLabel(BufferedImage originalImage, List<BoundingBoxInfo> boxesInfo) {
        BufferedImage imageWithBoxes = new BufferedImage(
                originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = imageWithBoxes.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.drawImage(originalImage, 0, 0, null);

        g2d.setColor(Color.BLUE);
        Font labelFont = new Font("Arial", Font.BOLD, 20);
        g2d.setFont(labelFont);

        for (BoundingBoxInfo boxInfo : boxesInfo) {
            int xMin = boxInfo.getCoord()[0];
            int yMin = boxInfo.getCoord()[1];
            int xMax = boxInfo.getCoord()[2];
            int yMax = boxInfo.getCoord()[3];
            String labelText = boxInfo.getEngLabel();

            int width = xMax - xMin;
            int height = yMax - yMin;

            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(4));

            g2d.drawRect(xMin, yMin, width, height);
            FontRenderContext frc = g2d.getFontRenderContext();
            TextLayout layout = new TextLayout(labelText, labelFont, frc);
            Rectangle2D bounds = layout.getBounds();

            int textWidth = (int) bounds.getWidth();
            int textHeight = (int) bounds.getHeight();
            int textX = xMin;
            int textY = yMin - 10;

            if (textY < 0) {
                textY = yMin + textHeight + 5;
            }

            g2d.setColor(new Color(0, 0, 0, 128));
            g2d.fillRect(textX, textY - textHeight, textWidth + 10, textHeight + 5);
            g2d.setColor(Color.WHITE);
            g2d.drawString(labelText, textX + 5, textY);
        }

        g2d.dispose();
        return imageWithBoxes;
    }

    private static class BoundingBoxInfo {
        private final int[] coord;
        private final String engLabel;

        public BoundingBoxInfo(int[] coord, String engLabel) {
            this.coord = coord;
            this.engLabel = engLabel;
        }

        public int[] getCoord() {
            return coord;
        }

        public String getEngLabel() {
            return engLabel;
        }
    }
}