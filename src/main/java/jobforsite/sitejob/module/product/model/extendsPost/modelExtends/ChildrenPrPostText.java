package jobforsite.sitejob.module.product.model.extendsPost.modelExtends;

import jakarta.persistence.*;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.interfaceFilter.PostDetailsVisitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "childrenpr_posts")
@DiscriminatorValue("CHILDRENPR")
public class ChildrenPrPostText extends PostText {


    @Column(name = "gender_childrenpr")
    private String genderChildrenPr;

    @Column(name = "condition_childrenpr")
    private String conditionChildrenPr;


    public <T> T accept(PostDetailsVisitor<T> visitor) {
        return visitor.visitChildrenPr(this);
    }

}