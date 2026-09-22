package jobforsite.sitejob.module.product.model.extendsPost.modelExtends;

import jakarta.persistence.*;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.interfaceFilter.PostDetailsVisitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "food_posts")
@DiscriminatorValue("FOOD")
public class FoodPostText extends PostText {


    @Column(name = "food_kind")
    private String foodKind;



    public <T> T accept(PostDetailsVisitor<T> visitor) {
        return visitor.visitFood(this);
    }
}