package com.project.cooksistant.model.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Data
@Table(name = "recipe_has_ingredient")
public class RecipeIngredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "recipeId",nullable = false)
    private Recipe recipe;


    @ManyToOne
    @JoinColumn(name = "ingredientId",nullable = false)
    private Ingredient ingredient;

    private String amount;
    @Column(name = "type",nullable = false)
    private String type;

}
