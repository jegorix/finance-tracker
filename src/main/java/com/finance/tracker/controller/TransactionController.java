package com.finance.tracker.controller;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.service.TransactionService;
import java.util.List;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    
    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public TransactionResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public List<TransactionResponse> getByCategory(@RequestParam String category)
    {
        return service.getByCategory(category);
    }
    

}
