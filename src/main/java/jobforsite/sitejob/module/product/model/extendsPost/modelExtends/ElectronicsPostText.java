package jobforsite.sitejob.module.product.model.extendsPost.modelExtends;

import jakarta.persistence.*;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.interfaceFilter.PostDetailsVisitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "electronics_posts")
@DiscriminatorValue("ELECTRONICS")
public class ElectronicsPostText extends PostText {


    @Column(name = "electronics_type")
    private String electronicsType;

    @Column(name = "condition_electronics")
    private String conditionElectronics;



    public <T> T accept(PostDetailsVisitor<T> visitor) {
        return visitor.visitElectronics(this);
    }
}