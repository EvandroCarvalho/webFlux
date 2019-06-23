package com.example.webflux.controller;


import com.example.webflux.model.Todo;
import com.example.webflux.repository.TodoRepository;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.Optional;


@RestController
@RequestMapping("/todos")
public class Controller {


    private TodoRepository todoRepository;
    private TransactionTemplate transactionTemplate;
    @Qualifier("jdbcScheduler")
    private Scheduler jdbcSchedulers;

    @Autowired
    Controller(TodoRepository todoRepository, TransactionTemplate transactionTemplate, Scheduler jdbcSchedulers) {
        this.todoRepository = todoRepository;
        this.transactionTemplate = transactionTemplate;
        this.jdbcSchedulers = jdbcSchedulers;
    }

    @GetMapping(path = "/hello")
    @ResponseBody
    public Publisher<String> sayHello() {
        return Mono.just("Hello World from Spring Web Flux");
    }

    @GetMapping
    @ResponseBody
    public Flux<Todo> findAll() {
        return Flux.defer(() -> Flux.fromIterable(this.todoRepository.findAll()).subscribeOn(jdbcSchedulers));
    }

    @GetMapping(path = "/{id}")
    @ResponseBody
    public Mono<Todo> findByid(@PathVariable("id") Long id) {
        return Mono.justOrEmpty(this.todoRepository.findById(id));
    }

    @PostMapping
    public Mono<Todo> save(@RequestBody Todo todo) {
        Mono op = Mono.fromCallable(() -> this.transactionTemplate.execute(action -> {
                    Todo newTodo = this.todoRepository.save(todo);
                    return newTodo;
                }));
        return op;
    }

    @DeleteMapping(path = "/{id}")
    public Mono<ResponseEntity<HttpStatus>> remove(@PathVariable("id") Long id) {
        return Mono.fromCallable(() -> this.transactionTemplate.execute(action -> {
            this.todoRepository.deleteById(id);
            return new ResponseEntity<HttpStatus>(HttpStatus.NO_CONTENT);
        })).subscribeOn(jdbcSchedulers);
    }




}
