package dev.danvega.courses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import dev.danvega.courses.service.ProductService;
import dev.danvega.courses.service.SellerService;

@SpringBootApplication
public class CoursesApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoursesApplication.class, args);
	}

	@Bean
	public List<ToolCallback> productTools(ProductService productService, SellerService sellerService) {
		List<ToolCallback> callbacks = new ArrayList<>();
		callbacks.addAll(Arrays.asList(ToolCallbacks.from(productService)));
		callbacks.addAll(Arrays.asList(ToolCallbacks.from(sellerService)));
		return callbacks;
	}
}
