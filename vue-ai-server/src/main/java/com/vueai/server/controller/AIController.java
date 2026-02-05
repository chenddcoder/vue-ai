package com.vueai.server.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/magic/ai")
@CrossOrigin
public class AIController {

    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody Map<String, String> body) {
        String prompt = body.get("prompt");
        // Mock AI response for now
        Map<String, Object> result = new HashMap<>();
        result.put("code", 1);
        Map<String, String> data = new HashMap<>();
        
        String template = "<template>\n" +
                "  <div class=\"generated-page\">\n" +
                "    <a-card title=\"" + prompt + "\">\n" +
                "      <p>This is an AI generated page based on your description.</p>\n" +
                "      <a-button type=\"primary\">Action</a-button>\n" +
                "    </a-card>\n" +
                "  </div>\n" +
                "</template>";
        String script = "export default {\n  data() {\n    return {\n    }\n  },\n  methods: {\n    init() {\n      console.log('Page loaded');\n    }\n  }\n}";
        String style = ".generated-page { padding: 20px; }";
        
        data.put("template", template);
        data.put("methods", script);
        data.put("style", style);
        
        result.put("data", data);
        return result;
    }
}
