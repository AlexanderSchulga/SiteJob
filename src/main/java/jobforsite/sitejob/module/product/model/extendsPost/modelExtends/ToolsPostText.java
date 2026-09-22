package jobforsite.sitejob.module.product.model.extendsPost.modelExtends;

import jakarta.persistence.*;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.interfaceFilter.PostDetailsVisitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tools_posts")
@DiscriminatorValue("TOOLS")
public class ToolsPostText extends PostText {


    @Column(name = "tools_type")
    private String toolsType;

    @Column(name = "condition_tools")
    private String conditionTools;



    public <T> T accept(PostDetailsVisitor<T> visitor) {
        return visitor.visitTools(this);
    }
}