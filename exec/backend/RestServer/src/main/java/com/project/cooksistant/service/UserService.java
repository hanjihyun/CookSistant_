package com.project.cooksistant.service;

import com.project.cooksistant.Exception.RestException;
import com.project.cooksistant.model.dto.PersonalDTO;
import com.project.cooksistant.model.dto.RecipeMypageDTO;
import com.project.cooksistant.model.dto.ScrapMypageDTO;
import com.project.cooksistant.model.dto.SignupDTO;
import com.project.cooksistant.model.entity.Recipe;
import com.project.cooksistant.model.entity.Scrap;
import com.project.cooksistant.model.entity.User;
import com.project.cooksistant.repository.RecipeRepository;
import com.project.cooksistant.repository.ScrapRepository;
import com.project.cooksistant.repository.UserRepository;
import com.project.cooksistant.s3.S3Uploader;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final ScrapRepository scrapRepository;
    private final ModelMapper modelMapper;
    @PersistenceContext
    EntityManager entityManager;

    public UserService(UserRepository userRepository, RecipeRepository recipeRepository, ScrapRepository scrapRepository, ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
        this.scrapRepository = scrapRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public PersonalDTO getUserData(String uid) {
        PersonalDTO personalDTO = new PersonalDTO();
        List<ScrapMypageDTO> scrapMypageDTOList = new ArrayList<>();
        List<RecipeMypageDTO> recipeMypageDTOList = new ArrayList<>();
        Optional<User> user = Optional.ofNullable(userRepository.findByUid(uid).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ????????? ???????????? ????????????.")));
        personalDTO.setUserId(user.get().getUserId());
        personalDTO.setNickname(user.get().getNickname());

        //?????? ????????? ????????? ??????
        int recipeSize = 0;
        List<Recipe> recipeList = recipeRepository.findAllByUser(user.get());
        for (int i = 0; i < recipeList.size(); i++) {
            if (recipeList.get(i).getFlag()) {
                recipeSize++;
                RecipeMypageDTO recipeMypageDTO = new RecipeMypageDTO();
                recipeMypageDTO.setRecipeId(recipeList.get(i).getRecipeId());
                recipeMypageDTO.setCuisine(recipeList.get(i).getCuisine());
                recipeMypageDTO.setDescription(recipeList.get(i).getDescription());
                if (recipeList.get(i).getImage().contains("https"))
                    recipeMypageDTO.setImage(recipeList.get(i).getImage());
                else
                    recipeMypageDTO.setImage("https://" + S3Uploader.CLOUD_FRONT_DOMAIN_NAME + "/" + recipeList.get(i).getImage());
                recipeMypageDTO.setUser(recipeList.get(i).getUser());
                recipeMypageDTOList.add(recipeMypageDTO);
            }
        }
        personalDTO.setRecipeSize(recipeSize);
        personalDTO.setRecipeList(recipeMypageDTOList);

//        recipeMypageDTOList = modelMapper.map(recipeList, new TypeToken<List<RecipeMypageDTO>>() {
//        }.getType());
//        personalDTO.setRecipeList(recipeMypageDTOList);

        //?????? ???????????? ????????? ??????
        List<Scrap> scrapList = scrapRepository.findAllByUser(user.get());
        int scrapCnt = 0;

        for (int i = 0; i < scrapList.size(); i++) {
            if (scrapList.get(i).getFlag()) {
                scrapCnt++;
                ScrapMypageDTO scrapMypageDTO = new ScrapMypageDTO();
                scrapMypageDTO.setNickname(scrapList.get(i).getUser().getNickname());
                scrapMypageDTO.setRecipeId(scrapList.get(i).getRecipe().getRecipeId());
                if (scrapList.get(i).getRecipe().getImage().contains("https"))
                    scrapMypageDTO.setImage(scrapList.get(i).getRecipe().getImage());
                else
                    scrapMypageDTO.setImage("https://" + S3Uploader.CLOUD_FRONT_DOMAIN_NAME + "/" + scrapList.get(i).getRecipe().getImage());
                scrapMypageDTO.setDescription(scrapList.get(i).getRecipe().getDescription());
                scrapMypageDTO.setCuisine(scrapList.get(i).getRecipe().getCuisine());
//                System.out.println(scrapMypageDTO);
                scrapMypageDTOList.add(scrapMypageDTO);
            }
        }
        personalDTO.setScrapSize(scrapCnt);
        personalDTO.setScrapList(scrapMypageDTOList);

        //??????????????? ????????? ??????
        String usedRecipeCount = "select count(e) from Evaluation e where e.user.uid= :uid and e.isComplete=true";
        Long used = entityManager.createQuery(usedRecipeCount, Long.class).setParameter("uid", uid).getSingleResult();
        personalDTO.setEvaluatedSize(used);

        //????????? ????????? ??????
        String evalCount = "select count(e) from Evaluation e where e.user.uid= :uid";
        Long eval = entityManager.createQuery(evalCount, Long.class).setParameter("uid", uid).getSingleResult();
        personalDTO.setRecipeUsedSize(eval);
        return personalDTO;
    }

    public ScrapMypageDTO scrapRecipe(Long recipeId, Long userId) {
        Optional<Recipe> recipe = Optional.ofNullable(recipeRepository.findById(recipeId).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ???????????? ???????????? ?????? ????????? ?????????.")));
        Optional<User> user = Optional.ofNullable(userRepository.findById(userId).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ????????? ???????????? ????????????.")));

        //?????? ???????????? ??????????????? ??????
        Optional<Scrap> isScrap = scrapRepository.findScrapByRecipeAndUserAndFlag(recipe, user, true);
        if (isScrap.isPresent()) {//?????? ???????????? ????????? ????????? ?????? ???????????????.
            throw new RestException(HttpStatus.BAD_REQUEST, "?????? ???????????? ????????? ?????????.");
        } else {
            Scrap scrap = new Scrap();
            scrap.setRecipe(recipe.get());
            scrap.setUser(user.get());
            scrap.setFlag(true);
            scrapRepository.save(scrap);

            ScrapMypageDTO scrapMypageDTO = new ScrapMypageDTO();
            scrapMypageDTO.setCuisine(recipe.get().getCuisine());
            scrapMypageDTO.setDescription(recipe.get().getDescription());
            scrapMypageDTO.setImage(recipe.get().getImage());
            scrapMypageDTO.setRecipeId(recipe.get().getRecipeId());
            scrapMypageDTO.setNickname(user.get().getNickname());
            return scrapMypageDTO;
        }
    }

    public Boolean signup(SignupDTO signupDTO) {
        System.out.println("????????? ????????? uid???:" + signupDTO.getUid());
        Optional<User> user = userRepository.findByUid(signupDTO.getUid());
        if (user.isPresent()) {
            return false;
        }
        User newuser = new User();
        newuser.setUid(signupDTO.getUid());
        newuser.setNickname(signupDTO.getNickname());
        userRepository.save(newuser);

        return true;
    }

    public Boolean myScrapData(Long userId, Long recipeId) {
        Optional<User> user = Optional.ofNullable(userRepository.findById(userId).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ????????? ???????????? ????????????.")));
        Optional<Recipe> recipe = Optional.ofNullable(recipeRepository.findById(recipeId).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ???????????? ???????????? ????????????.")));
        Optional<Scrap> scrap = scrapRepository.findScrapByRecipeAndUserAndFlag(recipe, user, true);
        if (scrap.isPresent())
            return true;
        else
            return false;
    }

    public Boolean deleteScrap(Long userid, Long recipeId) {
        Optional<User> user = Optional.ofNullable(userRepository.findById(userid).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ????????? ???????????? ????????????.")));
        Optional<Recipe> recipe = Optional.ofNullable(recipeRepository.findById(recipeId).orElseThrow(() -> new RestException(HttpStatus.NOT_FOUND, "?????? ???????????? ???????????? ????????????.")));
        Optional<Scrap> scrap = scrapRepository.findScrapByRecipeAndUserAndFlag(recipe, user, true);
        if (scrap.isPresent()) {
            scrap.get().setFlag(false);
            scrapRepository.save(scrap.get());
            return true;
        } else
            return false;
    }
}
