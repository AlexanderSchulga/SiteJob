package jobforsite.sitejob.module.product.model.extendsPost.modelExtends;

import jakarta.persistence.*;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.interfaceFilter.PostDetailsVisitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sports_posts")
@DiscriminatorValue("SPORTS")
public class SportsPostText extends PostText {


    @Column(name = "sports_kind")
    private String sportsKind;

    @Column(name = "sports_type")
    private String sportsType;

    @Column(name = "gender_sports")
    private String genderSports;

    @Column(name = "condition_sports")
    private String conditionSports;



    public <T> T accept(PostDetailsVisitor<T> visitor) {
        return visitor.visitSports(this);
    }
}