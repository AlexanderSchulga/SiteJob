package jobforsite.sitejob.module.product.model.extendsPost.modelExtends;

import jakarta.persistence.*;
import jobforsite.sitejob.module.product.model.interfaceFilter.PostDetailsVisitor;
import jobforsite.sitejob.module.product.model.PostText;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "vehicle_posts")
@DiscriminatorValue("VEHICLE")
public class MotorVehiclesPostText extends PostText {

    @Column(name = "vehicle_type")
    private String vehicleType; // cargo, construction, passenger, bus

    @Column(name = "vehicle_origin")
    private String vehicleOrigin; // europe, america, asia, cis

    @Column(name = "vehicle_year")
    private Integer vehicleYear;

    @Column(name = "mileage")
    private Integer mileage;

    @Column(name = "number_of_owners")
    private Integer numberOfOwners;


    public <T> T accept(PostDetailsVisitor<T> visitor) {
        return visitor.visitVehicle(this);
    }
}