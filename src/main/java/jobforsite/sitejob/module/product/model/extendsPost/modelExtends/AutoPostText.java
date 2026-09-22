package jobforsite.sitejob.module.product.model.extendsPost.modelExtends;

import jakarta.persistence.*;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.interfaceFilter.PostDetailsVisitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "auto_posts")
@DiscriminatorValue("AUTO")
public class AutoPostText extends PostText {


    @Column(name = "auto_type")
    private String autoType;

    @Column(name = "condition_auto")
    private String conditionAuto;



    public <T> T accept(PostDetailsVisitor<T> visitor) {
        return visitor.visitAuto(this);
    }
}