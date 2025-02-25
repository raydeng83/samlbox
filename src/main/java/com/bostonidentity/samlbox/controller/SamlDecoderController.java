package com.bostonidentity.samlbox.controller;

import com.bostonidentity.samlbox.model.SamlData;
import com.bostonidentity.samlbox.service.SamlDecoderUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

@Controller
public class SamlDecoderController {

    @GetMapping("/decoder")
    public String showDecoder() {
        return "saml-decoder";
    }

    @PostMapping("/decode")
    public String decodeSaml(
            @RequestParam String samlInput,
            Model model
    ) throws Exception {
        String decodedXml = SamlDecoderUtil.decodeSaml(samlInput);
        model.addAttribute("result", decodedXml);
        model.addAttribute("parsedData", parseSamlData(decodedXml));
        return "saml-decoder";
    }

    private SamlData parseSamlData(String xml) throws Exception {
        SamlData data = new SamlData();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

        // Parse XML elements
        data.setIssuer(getElementValue(doc, "Issuer"));
        data.setNameId(getElementValue(doc, "NameID"));
        data.setAudience(getElementValue(doc, "Audience"));
        data.setValidUntil(getElementValue(doc, "Conditions", "NotOnOrAfter"));

        return data;
    }

    private String getElementValue(Document doc, String... tagNames) {
        for (String tagName : tagNames) {
            NodeList nodes = doc.getElementsByTagNameNS("*", tagName);
            if (nodes.getLength() > 0) {
                return nodes.item(0).getTextContent();
            }
        }
        return null;
    }
}