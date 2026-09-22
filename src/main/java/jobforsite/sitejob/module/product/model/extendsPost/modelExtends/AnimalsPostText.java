package jobforsite.sitejob.module.product.model.extendsPost.modelExtends;

import jakarta.persistence.*;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.interfaceFilter.PostDetailsVisitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "animals_posts")
@DiscriminatorValue("ANIMALS")
public class AnimalsPostText extends PostText {


    @Column(name = "animals_kind")
    private String animalsKind;

    public <T> T accept(PostDetailsVisitor<T> visitor) {
        return visitor.visitAnimals(this);
    }
}