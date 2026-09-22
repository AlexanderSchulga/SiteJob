
package jobforsite.sitejob.module.product.model.extendsPost.modelExtends;

import jakarta.persistence.*;
import jobforsite.sitejob.module.product.model.interfaceFilter.PostDetailsVisitor;
import jobforsite.sitejob.module.product.model.PostText;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "realty_posts")
@DiscriminatorValue("REALTY")
public class RealEstatePostText extends PostText {

    // Тип недвижимости: apartment, house, land
    @Column(name = "property_type")
    private String propertyType;

    // Общие для квартир и домов
    @Column(name = "rooms_count")
    private Integer roomsCount;

    @Column(name = "floors_count")
    private Integer floorsCount;

    @Column(name = "floors_count_house")
    private Integer floorsCountHouse;

    @Column(name = "has_elevator")
    private Boolean hasElevator;

    @Column(name = "build_year")
    private Integer buildYear;



    // Для квартир
    @Column(name = "floors_apartment")
    private Integer floorsApartment;

    @Column(name = "material_up")
    private String materialUp;

    @Column(name = "repair_up")
    private String repairUp;

    // Для домов
    @Column(name = "house_area")
    private Integer houseArea;

    @Column(name = "land_area")
    private Integer landArea;

    @Column(name = "material_house")
    private String materialHouse;

    @Column(name = "repair_house")
    private String repairHouse;

    @Column(name = "distance_to_city_house")
    private Integer distanceToCityHouse;


    // Для участков
    @Column(name = "distance_to_city")
    private Integer distanceToCity;



    @Column(name = "land_area_garage")
    private Integer landAreaGarage;


    @Column(name = "land_area_commercial")
    private Integer landAreaCommercial;



    public <T> T accept(PostDetailsVisitor<T> visitor) {
        return visitor.visitRealty(this);
    }
}