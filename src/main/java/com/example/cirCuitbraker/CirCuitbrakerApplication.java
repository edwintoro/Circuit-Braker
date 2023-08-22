package com.example.cirCuitbraker;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class CirCuitbrakerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CirCuitbrakerApplication.class, args);
	}
	
	

@Bean 
ReactiveCircuitBreakerFactory reactiveCircuit(CircuitBreakerRegistry circuitBreakerRegistry) {
	var factory =  new ReactiveResilience4JCircuitBreakerFactory(circuitBreakerRegistry, null);
	
	factory.configureDefault(s -> new Resilience4JConfigBuilder(s)
			.timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(5)).build())
			.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults()).build());
	 
	 return factory;
	
}


	
	

}




@RestController
class FailingController {
	
	private final FailingService failingService ;
	
	private final ReactiveCircuitBreaker circuitBreaker;
	
	FailingController(FailingService fs,ReactiveCircuitBreakerFactory cbf){
		this.failingService = fs;
		this.circuitBreaker = cbf.create("greet");
	
	}
	
	@GetMapping("/greet")
	Mono<String> greet(@RequestParam Optional<String> name){
		
		var results = this.failingService.greet(name);
		return this.circuitBreaker.run(results, throwable -> Mono.just("Por favor ingrese un Parametro")); 
	
	}
}




@Service
class FailingService{
	Mono<String> greet(Optional<String> name){
		var seconds = (long) (Math.random()* 10);
		return name.map(str -> Mono.just("El parametro ingresado : " + str + "Segundos de la transaccion : " +   seconds )).orElse(Mono.error(new NullPointerException()))
				.delayElement(Duration.ofSeconds(seconds));
	}
	
	
}