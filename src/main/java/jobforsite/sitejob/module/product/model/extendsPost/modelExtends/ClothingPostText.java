package jobforsite.sitejob.module.product.model.extendsPost.modelExtends;

import jakarta.persistence.*;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.interfaceFilter.PostDetailsVisitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "clothing_posts")
@DiscriminatorValue("CLOTHING")
public class ClothingPostText extends PostText {


    @Column(name = "clothing_type")
    private String clothingType;

    @Column(name = "gender_clothing")
    private String genderClothing;

    @Column(name = "condition_clothing")
    private String conditionClothing;


    public <T> T accept(PostDetailsVisitor<T> visitor) {
        return visitor.visitClothing(this);
    }
}