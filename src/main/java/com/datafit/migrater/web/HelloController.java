package com.datafit.migrater.web;
import org.springframework.web.bind.annotation.GetMapping; import org.springframework.web.bind.annotation.RestController; import java.util.Map;
@RestController public class HelloController { @GetMapping("/") public Map<String,String> home(){ return Map.of("status","DataFit Migrater running"); } }
