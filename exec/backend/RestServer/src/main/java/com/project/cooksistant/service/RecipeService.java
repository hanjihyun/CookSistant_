package com.project.cooksistant.service;

import com.project.cooksistant.Exception.RestException;
import com.project.cooksistant.model.dto.*;
import com.project.cooksistant.model.entity.*;
import com.project.cooksistant.repository.*;
import com.project.cooksistant.s3.S3Uploader;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RecipeService {

    @PersistenceContext
    EntityManager entityManager;
    private final UserRepository userRepository;
    private final StepRepository stepRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final EvaluationRepository evaluationRepository;
    private final EvaluationKeywordRepository evaluationKeywordRepository;
    private final KeywordRepository keywordRepository;
    private final S3Uploader s3Uploader;

    public RecipeService(UserRepository userRepository, StepRepository stepRepository, RecipeRepository recipeRepository, IngredientRepository ingredientRepository, RecipeIngredientRepository recipeIngredientRepository, EvaluationRepository evaluationRepository, EvaluationKeywordRepository evaluationKeywordRepository, KeywordRepository keywordRepository, S3Uploader s3Uploader) {
        this.userRepository = userRepository;
        this.stepRepository = stepRepository;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.evaluationRepository = evaluationRepository;
        this.evaluationKeywordRepository = evaluationKeywordRepository;
        this.keywordRepository = keywordRepository;
        this.s3Uploader = s3Uploader;
    }


    public EvaluationDTO findEvaluation(Long evalId) {
        Optional<Evaluation> evaluation = Optional.ofNullable(evaluationRepository.findById(evalId).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ???????????? ?????? ?????? ????????????.")));
        List<EvaluationKeyword> evaluationKeywordList = evaluationKeywordRepository.findAllByEvaluation(evaluation);
        EvaluationDTO evaluationDTO = new EvaluationDTO();
//        evaluationDTO.setComplete(true);
        evaluationDTO.setSampled(evaluation.get().getIsSampled());
        evaluationDTO.setRecipeId(evaluation.get().getRecipe().getRecipeId());
        Optional<Recipe> recipe = recipeRepository.findById(evaluation.get().getRecipe().getRecipeId());
        Long recipeId = recipe.get().getRecipeId();
        evaluationDTO.setFavor(evaluation.get().getFavor());
        evaluationDTO.setUserId(evaluation.get().getUser().getUserId());
        for (int i = 0; i < evaluationKeywordList.size(); i++) {
            evaluationDTO.getKeywords().add(evaluationKeywordList.get(i).getKeyword().getKeyword());
        }

        return evaluationDTO;
    }

    public Long evaluateRecipe(EvaluationDTOpost evaluationDTOpost) {
        Optional<Recipe> recipe = Optional.ofNullable(recipeRepository.findById(evaluationDTOpost.getRecipeId()).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ???????????? ???????????? ????????????.")));
        Optional<User> user = Optional.ofNullable(userRepository.findById(evaluationDTOpost.getUserId()).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ????????? ???????????? ?????? ???????????????.")));
        Evaluation evaluation = new Evaluation();
        evaluation.setUser(user.get());
        evaluation.setIsSampled(true);
        evaluation.setRecipe(recipe.get());
        evaluation.setIsComplete(evaluationDTOpost.getIsComplete());//isComplete??? true??? ??????.
        evaluation.setFavor(evaluationDTOpost.getFavor());
//        if (evaluationDTOpost.getIsComplete()) {
//            evaluationRepository.save(evaluation);


//            for (int i = 0; i < evaluationDTOpost.getKeywordList().size(); i++) {
//                EvaluationKeyword evaluationKeyword = new EvaluationKeyword();
//                Keyword keyword = keywordRepository.findByKeyword(evaluationDTOpost.getKeywordList().get(i));//keyword ??????????????? ?????? ????????? ?????? ??????
//                evaluationKeyword.setEvaluation(evaluation); //?????? id??? ????????? ????????? ?????????????????? id??? ??????
//                evaluationKeyword.setKeyword(keyword);//?????? ???????????? ??????????????? keyword ????????? ??????
//                evaluationKeywordRepository.save(evaluationKeyword);
//            }
//        } else {
//        evaluation.setIsComplete(evaluationDTOpost.getIsComplete());//isComplete??? false??? ??????.
        evaluationRepository.save(evaluation);
//        }
        return evaluation.getEvalId();

    }

    //isComplete??? 0?????? 1?????? ?????? ??????
    public List<AllEvaluationDTO> findAllEvaluation(String uid) {
        Optional<User> user = Optional.ofNullable(userRepository.findByUid(uid).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ?????? ????????????????????????.")));
        List<Evaluation> evaluationList = evaluationRepository.findAllByUserOrderByIsCompleteAscEvalIdDesc(user.get()); // ????????? ????????? ?????? ????????? ???????????? ?????????

        List<AllEvaluationDTO> allEvaluationDTOList = new ArrayList<>();
        for (int i = 0; i < evaluationList.size(); i++) {
            AllEvaluationDTO allEvaluationDTO = new AllEvaluationDTO();
            allEvaluationDTO.setCuisine(evaluationList.get(i).getRecipe().getCuisine());
            allEvaluationDTO.setFavor(evaluationList.get(i).getFavor());
            allEvaluationDTO.setIsComplete(evaluationList.get(i).getIsComplete());
            allEvaluationDTO.setIsSampled(evaluationList.get(i).getIsSampled());
            allEvaluationDTO.setRecipe_id(evaluationList.get(i).getRecipe().getRecipeId());
            allEvaluationDTO.setImage("https://" + S3Uploader.CLOUD_FRONT_DOMAIN_NAME + "/" + evaluationList.get(i).getRecipe().getImage());
            allEvaluationDTO.setEvaluationId(evaluationList.get(i).getEvalId());
            allEvaluationDTOList.add(allEvaluationDTO);
        }
        return allEvaluationDTOList;
    }


    public RecipeDTO getRecommendation(Long recipeId) {
//        AmazonS3Client s3Client = (AmazonS3Client) AmazonS3ClientBuilder.defaultClient();
        //recipe_id??? ???????????? ??????
        Optional<Recipe> recipe = Optional.ofNullable(recipeRepository.findById(recipeId).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ???????????? ???????????? ????????????.")));
        RecipeDTO recipeDTO = new RecipeDTO();
        recipeDTO.setRecipeId(recipe.get().getRecipeId());
        recipeDTO.setNickname(recipe.get().getUser().getNickname());
        recipeDTO.setCuisine(recipe.get().getCuisine());
        recipeDTO.setDescription(recipe.get().getDescription());
        recipeDTO.setCookingTime(recipe.get().getCookingTime());
        recipeDTO.setImage("https://" + S3Uploader.CLOUD_FRONT_DOMAIN_NAME + "/" + recipe.get().getImage());
        recipeDTO.setLevel(recipe.get().getLevel());
        recipeDTO.setServing(recipe.get().getServing());

        //?????? ???????????? id??? ingredientId??? ????????????.
        List<IngredientDTO> ingredientDTOList = new ArrayList<>();
        List<RecipeIngredient> recipeIngredient = recipeIngredientRepository.findAllByRecipe(recipe); //recipe_has_ingredient table
        for (int j = 0; j < recipeIngredient.size(); j++) {
            Ingredient ingredient = ingredientRepository.findByIngredientId(recipeIngredient.get(j).getIngredient().getIngredientId());// recipe_has_ingredient??????????????? ?????? ingredient_id??? ?????? ???????????? ????????????.
            IngredientDTO ingredientDTO = new IngredientDTO();
            ingredientDTO.setIngredientId(ingredient.getIngredientId());
            ingredientDTO.setIngredientName(ingredient.getIngredientName());
            ingredientDTO.setAmount(recipeIngredient.get(j).getAmount());
            ingredientDTO.setIsType(recipeIngredient.get(j).getType());
            ingredientDTOList.add(ingredientDTO);
        }
        recipeDTO.setIngredientDTOList(ingredientDTOList); // ?????? ????????? ?????????.

        //????????? ?????????.
        List<Step> stepList = stepRepository.findAllByRecipe(recipe);
        List<StepDTO> stepDTOList = new ArrayList<>();

        for (int j = 0; j < stepList.size(); j++) {
            StepDTO stepDTO = new StepDTO();
            stepDTO.setStepId(stepList.get(j).getStepId());
            stepDTO.setDescription(stepList.get(j).getDescription());
            if (stepList.get(j).getImage().contains("http"))
                stepDTO.setImage(stepList.get(j).getImage());
            else
                stepDTO.setImage("https://" + S3Uploader.CLOUD_FRONT_DOMAIN_NAME + "/" + stepList.get(j).getImage());
            stepDTO.setLevel(stepList.get(j).getLevel());
            stepDTOList.add(stepDTO);
        }
        recipeDTO.setStepList(stepDTOList);
        return recipeDTO;
    }

    public List<RecipeListupDTO> recommendList(List<Long> recommendList) {
        List<RecipeListupDTO> recipeListupDTOList = new ArrayList<>();
        for (int i = 0; i < recommendList.size(); i++) {
            RecipeListupDTO recipeListupDTO = new RecipeListupDTO();
            Optional<Recipe> recipe = recipeRepository.findById(recommendList.get(i));
            recipeListupDTO.setRecipeId(recipe.get().getRecipeId());
            recipeListupDTO.setRecipename(recipe.get().getCuisine());
            recipeListupDTO.setUrl("https://" + S3Uploader.CLOUD_FRONT_DOMAIN_NAME + "/" + recipe.get().getImage());
            recipeListupDTO.setDescription(recipe.get().getDescription());
            Long recipeId = recipe.get().getRecipeId();
            //????????????
            String query = "select avg(e.favor) from Evaluation e where e.recipe.recipeId= :recipeId";
            Double avg_favor = entityManager.createQuery(query, Double.class).setParameter("recipeId", recipeId).getSingleResult();

            recipeListupDTO.setFavor(avg_favor);
            recipeListupDTOList.add(recipeListupDTO);
        }
        return recipeListupDTOList;
    }

    public List<RecipeListupDTO> recipeFavor() {
        List<RecipeListupDTO> recipeListupDTOList = new ArrayList<>();
        String jpql = "select distinct(e.recipe.recipeId) from Evaluation e where e.recipe.flag=true order by e.favor desc";
        List<Long> recipeIds = entityManager.createQuery(jpql, Long.class).getResultList();
        for (int i = 0; i < recipeIds.size(); i++) {
            RecipeListupDTO recipeListupDTO = new RecipeListupDTO();
            Optional<Recipe> recipe = recipeRepository.findById(recipeIds.get(i));
            recipeListupDTO.setRecipeId(recipe.get().getRecipeId());
            if (recipe.get().getImage().contains("http"))
                recipeListupDTO.setUrl(recipe.get().getImage());
            else
                recipeListupDTO.setUrl("https://" + S3Uploader.CLOUD_FRONT_DOMAIN_NAME + "/" + recipe.get().getImage());
            recipeListupDTO.setDescription(recipe.get().getDescription());
            recipeListupDTO.setRecipename(recipe.get().getCuisine());
            Long recipeId = recipe.get().getRecipeId();
            //????????????
            String query = "select avg(e.favor) from Evaluation e where e.recipe.recipeId= :recipeId";
            Double avg_favor = entityManager.createQuery(query, Double.class).setParameter("recipeId", recipeId).getSingleResult();
            recipeListupDTO.setFavor(avg_favor);
            recipeListupDTOList.add(recipeListupDTO);
        }
        return recipeListupDTOList;
    }

    public boolean evaluateUpdate(EvaluationDTOpostUpdate evaluationDTOpost) {
        Optional<Evaluation> evaluation = Optional.ofNullable(evaluationRepository.findById(evaluationDTOpost.getEvaluationId()).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ?????????????????? ????????????????????????.")));
        evaluation.get().setIsComplete(evaluationDTOpost.getIsComplete());
        evaluation.get().setFavor(evaluationDTOpost.getFavor());
        for (int i = 0; i < evaluationDTOpost.getKeywordList().size(); i++) {
            EvaluationKeyword evaluationKeyword = new EvaluationKeyword();
            Keyword keyword = keywordRepository.findByKeyword(evaluationDTOpost.getKeywordList().get(i));//keyword ??????????????? ?????? ????????? ?????? ??????
            evaluationKeyword.setEvaluation(evaluation.get()); //?????? id??? ????????? ????????? ?????????????????? id??? ??????
            evaluationKeyword.setKeyword(keyword);//?????? ???????????? ??????????????? keyword ????????? ??????
            evaluationKeywordRepository.save(evaluationKeyword);
        }
        evaluationRepository.save(evaluation.get());
        return true;
    }

//    public void recipeClick(RecipeClickDTO recipeClickDTO) {
//        Optional<User> user = Optional.ofNullable(userRepository.findById(recipeClickDTO.getUserId()).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "??????????????? ????????????????????????.")));
//        Optional<Recipe> recipe = Optional.ofNullable(recipeRepository.findById(recipeClickDTO.getRecipeId()).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ???????????? ???????????? ????????????.")));
//        Evaluation evaluation = new Evaluation();
//        evaluation.setIsSampled(true);
//        evaluation.setUser(user.get());
//        evaluation.setRecipe(recipe.get());
//    }

    public List<RecipeListupDTO> search(String cuisine) {
        List<RecipeListupDTO> recipeListupDTOList = new ArrayList<>();
        List<Recipe> recipeList = recipeRepository.findByCuisineContaining(cuisine);
//        System.out.println(recipeList.size());
        for (int i = 0; i < recipeList.size(); i++) {
            RecipeListupDTO recipeListupDTO = new RecipeListupDTO();
            recipeListupDTO.setRecipename(recipeList.get(i).getCuisine());
            recipeListupDTO.setUrl("https://" + S3Uploader.CLOUD_FRONT_DOMAIN_NAME + "/" + recipeList.get(i).getImage());
            recipeListupDTO.setRecipeId(recipeList.get(i).getRecipeId());
            recipeListupDTO.setDescription(recipeList.get(i).getDescription());
            Long recipeId = recipeList.get(i).getRecipeId();
            //????????????
            String query = "select avg(e.favor) from Evaluation e where e.recipe.recipeId= :recipeId";
            Double avg_favor = entityManager.createQuery(query, Double.class).setParameter("recipeId", recipeId).getSingleResult();
            recipeListupDTO.setFavor(avg_favor);
            recipeListupDTOList.add(recipeListupDTO);
        }
        return recipeListupDTOList;
    }

    public int evaluationExist(Long userId) {
        Optional<User> user = Optional.ofNullable(userRepository.findById(userId).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ????????? ???????????? ????????????.")));
        Optional<List<Evaluation>> evaluationList = Optional.ofNullable((List<Evaluation>) evaluationRepository.findByUser(user.get()));
        if (evaluationList.get().size() > 0)
            return 1;
        else return 0;
    }


    public Long newRecipe(RecipeDTOpost recipeDTOpost) throws IOException {
        //?????? ????????? ??????????????? ??????
        Optional<User> user = Optional.ofNullable(userRepository.findByUid(recipeDTOpost.getUid()).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ????????? ???????????? ????????????.")));
        //1. recipe ???????????? ??????
        Recipe recipe = new Recipe();
        recipe.setUser(user.get());
        recipe.setServing(recipeDTOpost.getServing());
        recipe.setLevel(recipeDTOpost.getLevel());
        recipe.setCookingTime(recipeDTOpost.getCookingTime());
        recipe.setDescription(recipeDTOpost.getDescription());
        recipe.setCuisine(recipeDTOpost.getCuisine());
        recipe.setFlag(true);
        recipeRepository.save(recipe);
        //3. recipe_has_ingredient ???????????? ??????
        List<IngredientDTOpost> ingredientList = recipeDTOpost.getIngredientDTOpostList();
        for (int i = 0; i < ingredientList.size(); i++) {
            String ingredientName = ingredientList.get(i).getIngredientName();
            Optional<Ingredient> ing = Optional.ofNullable(ingredientRepository.findByIngredientName(ingredientName));
            RecipeIngredient recipeIngredient = new RecipeIngredient();
            //2. ?????? ????????? ?????????????????? ????????? ?????? ???????????? ?????? ??????
            if (!ing.isPresent()) {//??????????????? ?????????????????? ?????????
                Ingredient ingredient = new Ingredient();
                ingredient.setIngredientName(ingredientName);
                ingredientRepository.save(ingredient);
                recipeIngredient.setIngredient(ingredient);
            } else {
                recipeIngredient.setIngredient(ing.get());
            }
            recipeIngredient.setType(ingredientList.get(i).getIsType());
            recipeIngredient.setAmount(ingredientList.get(i).getAmount());
            recipeIngredient.setRecipe(recipe);
            recipeIngredientRepository.save(recipeIngredient);
        }

        //4. step ???????????? ??????
        List<StepDTOpost> stepDTOList = recipeDTOpost.getStepDTOpostList();
        for (int i = 0; i < stepDTOList.size(); i++) {
            Step step = new Step();
            step.setRecipe(recipe);
            step.setLevel((long) (i + 1));
            step.setDescription(stepDTOList.get(i).getStepDescription());
            if (stepDTOList.get(i).getImage() == null)
                step.setImage("no image");
            else
                step.setImage(stepDTOList.get(i).getImage());
            stepRepository.save(step);
        }
        return recipe.getRecipeId();
    }

    public List<String> allIngredient() {
        String jpql = "select i.ingredientName from Ingredient i";
        return entityManager.createQuery(jpql, String.class).getResultList();
    }

    public void mainImage(String originalName, MultipartFile file, Long recipeId) throws IOException {
        Optional<Recipe> recipe = recipeRepository.findById(recipeId);
        //5.s3??? ????????? ????????? ??? recipe image??? set??????.
        String image = s3Uploader.upload(originalName, file, recipeId);
        System.out.println(image + "?????????");
        recipe.get().setImage(image);
        recipeRepository.save(recipe.get());
    }

    public void basicFix(RecipeBasicFixDTO recipeBasicFixDTO) {
        Optional<Recipe> recipe = Optional.ofNullable(recipeRepository.findById(recipeBasicFixDTO.getRecipId()).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ???????????? ???????????? ????????????.")));
        recipe.get().setCuisine(recipeBasicFixDTO.getCuisine());
        recipe.get().setDescription(recipeBasicFixDTO.getDescription());
        recipe.get().setCookingTime(recipeBasicFixDTO.getCookingTime());
        recipeRepository.save(recipe.get());
    }

    public void stepFix(RecipeStepFixDTO recipeStepFixDTO) {
        Optional<Step> step = Optional.ofNullable(stepRepository.findById(recipeStepFixDTO.getStepId()).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ????????? ???????????? ????????????.")));
        step.get().setDescription(recipeStepFixDTO.getDescription());
        step.get().setLevel(recipeStepFixDTO.getLevel());
        stepRepository.save(step.get());
    }

    public void stepImage(String originalName, MultipartFile file, Long recipeId, Long level) throws IOException {
        Optional<Recipe> recipe = Optional.ofNullable(recipeRepository.findById(recipeId).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ???????????? ???????????? ????????????.")));
        Step step = stepRepository.findAllByRecipeAndLevel(recipe, level);

        String image = s3Uploader.uploadStep(originalName, file, step.getStepId());
        step.setImage(image);
        stepRepository.save(step);
    }

    public void ingredientFix(RecipeIngredientFixDTO recipeIngredientFixDTO) {
        Optional<RecipeIngredient> recipeIngredient = Optional.ofNullable(recipeIngredientRepository.findById(recipeIngredientFixDTO.getRecipeIngredientId()).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ?????? ????????? ???????????? ????????????.")));
        recipeIngredient.get().setAmount(recipeIngredientFixDTO.getAmount());
        recipeIngredient.get().setType(recipeIngredientFixDTO.getType());
        recipeIngredientRepository.save(recipeIngredient.get());
    }

    public void deleteRecipe(Long recipeId) {
        Optional<Recipe> recipe = Optional.ofNullable(recipeRepository.findById(recipeId).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ???????????? ???????????? ????????????.")));
        recipe.get().setFlag(false);
        recipeRepository.save(recipe.get());
    }

}