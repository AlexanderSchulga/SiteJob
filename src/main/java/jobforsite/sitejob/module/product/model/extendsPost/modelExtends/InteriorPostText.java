package jobforsite.sitejob.module.product.model.extendsPost.modelExtends;

import jakarta.persistence.*;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.interfaceFilter.PostDetailsVisitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "interior_posts")
@DiscriminatorValue("INTERIOR")
public class InteriorPostText extends PostText {


    @Column(name = "interior_type")
    private String interiorType;

    @Column(name = "condition_interior")
    private String conditionInterior;



    public <T> T accept(PostDetailsVisitor<T> visitor) {
        return visitor.visitInterior(this);
    }
}