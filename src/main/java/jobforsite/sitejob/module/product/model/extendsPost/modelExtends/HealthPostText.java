package jobforsite.sitejob.module.product.model.extendsPost.modelExtends;

import jakarta.persistence.*;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.interfaceFilter.PostDetailsVisitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "health_posts")
@DiscriminatorValue("HEALTH")
public class HealthPostText extends PostText {


    @Column(name = "health_kind")
    private String healthKind;

    @Column(name = "health_type")
    private String healthType;

    @Column(name = "gender_health")
    private String genderHealth;


    public <T> T accept(PostDetailsVisitor<T> visitor) {
        return visitor.visitHealth(this);
    }
}