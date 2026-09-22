package jobforsite.sitejob.module.product.model.extendsPost.modelExtends;

import jakarta.persistence.*;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.interfaceFilter.PostDetailsVisitor;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "constructions_posts")
@DiscriminatorValue("CONSTRUCTIONS")
public class ConstructionsPostText extends PostText {

    @Column(name = "constructions_type")
    private String constructionsType;


    public <T> T accept(PostDetailsVisitor<T> visitor) {
        return visitor.visitConstructions(this);
    }
}
