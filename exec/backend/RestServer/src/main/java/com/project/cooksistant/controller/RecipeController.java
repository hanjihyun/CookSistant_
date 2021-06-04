package com.project.cooksistant.controller;


import com.google.gson.*;
import com.project.cooksistant.model.dto.*;
import com.project.cooksistant.service.RecipeService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;

@RestController
public class RecipeController {
    private final RecipeService recipeService;
    private final WebClient webClient;

    public RecipeController(RecipeService recipeService, WebClient.Builder webClientBuilder) {
        this.recipeService = recipeService;
        this.webClient = webClientBuilder.baseUrl("http://j4c101.p.ssafy.io:5000").build();
    }

    @ApiOperation(value = "레시피 등록", notes = "Request\n" +
            "                                 - cookingTime: 조리시간\n" +
            "                                 - cuisine: 레시피명\n" +
            "                                 - description: 레시피 설명\n" +
            "                                 - uid: 등록하는 유저 uid\n" +
            "                                 - image: 레시피 사진 url\n" +
            "                                 - level: 난이도\n" +
            "                                 - serving: 인분\n" +
            "                                 - {\n" +
            "                                 - stepdescription: 과정 설명\n" +
            "                                 - image: 과정 사진\n" +
            "                                 - level: 과정 난이도\n" +
            "                                 -}\n" +
            "                                 - {\n" +
            "                                 - ingredientName:재료명\n" +
            "                                 - amount:재료량\n" +
            "                                 - isType: 재료 구분")
    @PostMapping(value = "/recipe/create")
    public Long newRecipe(@RequestBody RecipeDTOpost recipeDTOpost) throws IOException {
        return recipeService.newRecipe(recipeDTOpost);
    }

    @ApiOperation(value = "레시피 메인 사진 S3업로드 및 컬럼 값 설정", notes = "Request\n" +
            "                                                   - file: 사진\n" +
            "                                                   - recipeId: 레시피 인덱스")
    @PostMapping(value = "/recipe/mainImage")
    public void mainImage(@RequestParam("file") MultipartFile file, @RequestParam String originalName, @RequestParam Long recipeId) throws IOException {
        System.out.println(file.getOriginalFilename() + "=======================");
        recipeService.mainImage(originalName, file, recipeId);
    }

    @ApiOperation(value = "레시피 기본 내용 수정(레시피 명,설명,조리시간)")
    @PutMapping("/recipe/fjx/basic")
    public void fixBasicRecipe(@RequestBody RecipeBasicFixDTO recipeBasicFixDTO) {
        recipeService.basicFix(recipeBasicFixDTO);
    }

    @ApiOperation(value = "레시피과정 수정하기(설명,레벨)", notes = "Request\n" +
            "                                                       - stepId:과정 인덱스\n" +
            "                                                       - description: 과정 설명\n" +
            "                                                       - leve: 과정 레벨")
    @PutMapping("/recipe/fix/step")
    public void fixStepRecipe(@RequestBody RecipeStepFixDTO recipeStepFixDTO) {
        recipeService.stepFix(recipeStepFixDTO);
    }

    @ApiOperation(value = "레시피과정 이미지 등록")
    @PutMapping("/recipe/stepImage")
    public void stepImage(@RequestParam String originalName, @RequestParam("stepfile") MultipartFile file, @RequestParam("recipeId") Long recipeId, @RequestParam("level") Long level) throws IOException {
        recipeService.stepImage(originalName, file, recipeId, level);
    }

    @ApiOperation(value = "레시피 재료 수정하기", notes = "Request\n" +
            "                                          - recipeIngredientId: recipe_has_ingredient 인덱스\n" +
            "                                          - amount: 재료양\n" +
            "                                          - type: 재료 타입")
    @PutMapping("/recipe/fix/ingredient")
    public void fixIngredientRecipe(@RequestBody RecipeIngredientFixDTO recipeIngredientFixDTO) {
        recipeService.ingredientFix(recipeIngredientFixDTO);
    }

    @ApiOperation(value = "레시피 삭제")
    @PutMapping("/recipe/delete/{recipeId}")
    public void deleteRecipe(@PathVariable Long recipeId) {
        recipeService.deleteRecipe(recipeId);
    }

    @ApiOperation(value = "인기레시피")
    @GetMapping("/recipe/favor")
    public List<RecipeListupDTO> favorRecipe() {
        return recipeService.recipeFavor();
    }

    @ApiOperation(value = "취향 기반 레시피 리스트 제공(Ok) 신규유저라면 평가 데이터가 없으므로 인기순, 그 외 평가 데이터가 존재하는 유저는 추천을받는다", notes = "Request\n" +
            "                                                   - userId:협업필터링에 사용될 유저와 비슷한 레시피 추천을 위한 UserId\n" +
            "                                                   - List<String>: 추천받을 재료 리스트\n" +
            "                                                   Response\n" +
            "                                                   -List<RecipeListupDTO>:레시피 리스트\n" +
            "                                                   {\n" +
            "                                                   - url: 이미지 주소\n" +
            "                                                   - recipename: 레시피 이름\n" +
            "                                                   - recipeId: 레시피 아이디")
    @PostMapping("/recipe/recommendation")
    public List<RecipeListupDTO> recommend(@RequestBody RecommendDTO recommendDTO) {
        Gson gson = new Gson();
        String jsonArray = (webClient.post()
                .uri("/evaluation")
                .body(Mono.just(recommendDTO), RecommendDTO.class)
                .retrieve()
                .bodyToMono(String.class).block());
        JsonObject jsonObject = gson.fromJson(jsonArray, JsonObject.class);
        String[] idx = gson.fromJson(jsonObject.getAsJsonArray("result"), String[].class);
        List<Long> recommendList = new ArrayList<>();
        for (int i = 0; i < idx.length; i++) {
            recommendList.add(Long.parseLong(idx[i]));
        }
        return recipeService.recommendList(recommendList);

    }

    @ApiOperation(value = "OCR로 영수증 재료 인식 후 레시피 추천", notes = "Request\n" +
            "                                                       - userId:유저 인덱스\n" +
            "                                                       - ocrscan: 스캔을 통해 출력된 문자열\n" +
            "                                                   Response\n" +
            "                                                   -List<RecipeListupDTO>:레시피 리스트\n" +
            "                                                   {\n" +
            "                                                   - url: 이미지 주소\n" +
            "                                                   - recipename: 레시피 이름\n" +
            "                                                   - recipeId: 레시피 아이디")
    @PostMapping("/recipe/ocr/")
    public List<RecipeListupDTO> ocrIngredient(@RequestBody OcrDTO ocrDTO) {
        if (ocrDTO.getOcrscan().contains("물품"))
            ocrDTO.setOcrscan(ocrDTO.getOcrscan().replaceAll("물품", ""));
        System.out.println(ocrDTO.getOcrscan());
        Gson gson = new Gson();
        String jsonArray = (webClient.post()
                .uri("/konlypy")
                .body(Mono.just(ocrDTO), OcrDTO.class)
                .retrieve()
                .bodyToMono(String.class).block());
//        System.out.println(jsonArray);
        JsonObject jsonObject = gson.fromJson(jsonArray, JsonObject.class);
        String[] idx = gson.fromJson(jsonObject.getAsJsonArray("result"), String[].class);
        List<Long> recommendList = new ArrayList<>();
        for (int i = 0; i < idx.length; i++) {
            recommendList.add(Long.parseLong(idx[i]));
        }
        return recipeService.recommendList(recommendList);
    }

    @ApiOperation(value = "특정 레시피 상세보기(Ok)", notes = "Request\n" +
            "                                         - recipeId: 레시피 아이디\n" +
            "                                         Response\n" +
            "                                         - recipeId:레시피아이디\n" +
            "                                         - nickname:작성자 닉네임\n" +
            "                                         - cuisine:레시피명\n" +
            "                                         - description: 레시피 설명\n" +
            "                                         - cookingtime: 조리시간\n" +
            "                                         - image:사진 url\n" +
            "                                         - level: 난이도\n" +
            "                                         - serving: 인분\n" +
            "                                         - List<IngredientDTO>: 재료 리스트\n" +
            "                                         - List<StepDTO>:단계별 조리법")
    @GetMapping("/recipe/show/{recipeId}")
    public RecipeDTO showRecipe(@PathVariable Long recipeId) {
        return recipeService.getRecommendation(recipeId);
    }

    @ApiOperation(value = "레시피 평가하기(Ok)", notes = "Request\n" +
            "                                          - complete: 레시피 리뷰 작성 여부\n" +
            "                                          - keywordList: 평가 키워드 리스트\n" +
            "                                          - favor: 평점" +
            "                                          - recipeId: 평가 레시피 번호\n" +
            "                                          - isUpdate: 새로작성인지 수정인지\n" +
            "                                          - userId: 평가할 유저의 userId")
    @PostMapping("/recipe/evaluation")
    public Long evaluation(@RequestBody EvaluationDTOpost evaluationDTOpost) {
        return recipeService.evaluateRecipe(evaluationDTOpost);
    }

    @ApiOperation(value = "평가하지 않은 리뷰 평가하기")
    @PutMapping("/recipe/evaluationUpdate")
    public String evaluationUpdate(@RequestBody EvaluationDTOpostUpdate evaluationDTOpost) {
        boolean isEvaluation = recipeService.evaluateUpdate(evaluationDTOpost);
        if (isEvaluation)
            return "success";
        else
            return "fail";
    }


    @ApiOperation(value = "특정 레시피 평가내용 보기(Ok)", notes = "Request\n" +
            "                                                 - evalId: 평가 번호\n" +
            "                                                 Response\n" +
            "                                                 - complete: 레시피 리뷰 작성 여부\n" +
            "                                                 - keywords: 키워드 리스트\n" +
            "                                                 - recipeid: 레시피 번호\n" +
            "                                                 - userId: 유저번호")
    @GetMapping("/recipe/evaluation/{evalId}")
    public EvaluationDTO specificEvaluation(@PathVariable Long evalId) {
        return recipeService.findEvaluation(evalId);
    }


    @ApiOperation(value = "내가 리뷰한 혹은 리뷰하지 않은 레시피 리스트 (Ok)", notes = "Request\n" +
            "                                                           - uid: 유저 인증키\n" +
            "                                                           Response\n" +
            "                                                           - evaluationId:평가 인덱스\n" +
            "                                                           - cuisine: 레시피 명\n" +
            "                                                           - recipe_id: 레시피 번호\n" +
            "                                                           - favor: 평점\n" +
            "                                                           - isSampled:샘플링 여부\n" +
            "                                                           - isComplete: 레시피 리뷰 작성 여부\n" +
            "                                                           - image: recipe 사진")
    @PostMapping("/recipe/review/{uid}")
    public List<AllEvaluationDTO> viewRecipe(@PathVariable String uid) {
        return recipeService.findAllEvaluation(uid);
    }

//    @ApiOperation(value = "레시피 클릭시 추천 데이터로 포함")
//    @PostMapping("/recipe/click")
//    public void recipeClick(@RequestBody RecipeClickDTO recipeClickDTO) {
//        recipeService.recipeClick(recipeClickDTO);
//    }

    @ApiOperation(value = "레시피 검색", notes = "Request\n" +
            "                                  - Cuisine: 레시피 명\n" +
            "                                  Response:" +
            "                                 - recipename: 레시피명\n" +
            "                                 - url: 사진 url\n" +
            "                                 - recipeId: 레시피 번호")
    @GetMapping("/recipe/search/{cuisine}")
    public List<RecipeListupDTO> searchRecipe(@PathVariable String cuisine) {
        return recipeService.search(cuisine);
    }

    @ApiOperation(value = "전체 재료 리스트", notes = "전체 재료 리스트를 리턴해준다.")
    @GetMapping("/recipe/ingredient")
    public List<String> allIngredient() {
        return recipeService.allIngredient();
    }
}
