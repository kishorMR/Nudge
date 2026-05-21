package com.nudge.daily_tracker.controller;

import com.nudge.daily_tracker.model.BriefRequest;
import com.nudge.daily_tracker.model.BriefResponse;
import com.nudge.daily_tracker.service.BriefService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BriefController {

    @Autowired
    private BriefService briefService;

    @PostMapping("/daily-brief")
    public BriefResponse getDailyBrief(@RequestBody BriefRequest request) {
        String message = briefService.generateBrief(request);
        return new BriefResponse(message);
    }
}