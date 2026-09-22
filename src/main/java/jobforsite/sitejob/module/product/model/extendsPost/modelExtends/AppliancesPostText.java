package jobforsite.sitejob.module.product.model.extendsPost.modelExtends;

import jakarta.persistence.*;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.interfaceFilter.PostDetailsVisitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "appliances_posts")
@DiscriminatorValue("APPLIANCES")
public class AppliancesPostText extends PostText {


    @Column(name = "appliances_type")
    private String appliancesType;

    @Column(name = "condition_appliances")
    private String conditionAppliances;



    public <T> T accept(PostDetailsVisitor<T> visitor) {
        return visitor.visitAppliances(this);
    }
}