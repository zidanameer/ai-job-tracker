package com.jobtracker.controller;

import com.jobtracker.dto.AnalyzeRequest;
import com.jobtracker.dto.AnalyzeResponse;
import com.jobtracker.service.AnalysisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private final AnalysisService analysisService;

    public AnalyzeController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/analyze")
    public AnalyzeResponse analyze(@Valid @RequestBody AnalyzeRequest request) {
        return analysisService.analyze(request);
    }
}
