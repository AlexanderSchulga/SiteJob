package jobforsite.sitejob.module.product.mapper;

import jobforsite.sitejob.module.product.dto.PostCreateRequestDTO;
import jobforsite.sitejob.module.product.dto.PostCreationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * Маппер для преобразования DTO создания поста в данные для создания.
 * <p>
 * Преобразует {@link PostCreateRequestDTO} в {@link PostCreationData}.
 * Заполняет все поля, включая специфичные для различных категорий:
 * недвижимость, автотранспорт, автозапчасти, строительные материалы и т.д.
 * </p>
 *
 * @see PostCreateRequestDTO
 * @see PostCreationData
 */

@Component
@Slf4j
public class PostCreateRequestMapper {

    public PostCreationData toCreationData(PostCreateRequestDTO request) {
        PostCreationData data = new PostCreationData();

        // Общие поля
        data.setTitle(request.getTitle());
        data.setPrice(request.getPrice());
        data.setFullText(request.getFullText());
        data.setCategory(request.getCategory());
        data.setLocation(request.getLocation());
        data.setModeration(request.getModeration() != null ? request.getModeration() : true);

        // Realty
        data.setPropertyType(request.getPropertyType());
        data.setRoomsCount(request.getRoomsCount());
        data.setFloorsApartment(request.getFloorsApartment());
        data.setFloorsCount(request.getFloorsCount());
        data.setFloorsCountHouse(request.getFloorsCountHouse());
        data.setHasElevator(request.getHasElevator());
        data.setBuildYear(request.getBuildYear());
        data.setHouseArea(request.getHouseArea());
        data.setLandArea(request.getLandArea());
        data.setDistanceToCity(request.getDistanceToCity());
        data.setMaterialUp(request.getMaterialUp());
        data.setRepairUp(request.getRepairUp());
        data.setMaterialHouse(request.getMaterialHouse());
        data.setRepairHouse(request.getRepairHouse());
        data.setDistanceToCityHouse(request.getDistanceToCityHouse());
        data.setLandAreaGarage(request.getLandAreaGarage());
        data.setLandAreaCommercial(request.getLandAreaCommercial());


        // Vehicle
        data.setVehicleType(request.getVehicleType());
        data.setVehicleOrigin(request.getVehicleOrigin());
        data.setVehicleYear(request.getVehicleYear());
        data.setMileage(request.getMileage());
        data.setNumberOfOwners(request.getNumberOfOwners());


        //Auto
        data.setAutoType(request.getAutoType());
        data.setConditionAuto(request.getConditionAuto());

        //Строительные материалы--

        data.setConstructionsType(request.getConstructionsType());

        //-----Дом и интерьер

        data.setInteriorType(request.getInteriorType());
        data.setConditionInterior(request.getConditionInterior());

        //-----------Инструменты

        data.setToolsType(request.getToolsType());
        data.setConditionTools(request.getConditionTools());

        //-------Бытовая техника--
        data.setAppliancesType(request.getAppliancesType());
        data.setConditionAppliances(request.getConditionAppliances());

        //-----Электроника и гаджеты-----
        data.setElectronicsType(request.getElectronicsType());
        data.setConditionElectronics(request.getConditionElectronics());

        //--------Одежда и обувь
        data.setClothingType(request.getClothingType());
        data.setGenderClothing(request.getGenderClothing());
        data.setConditionClothing(request.getConditionClothing());

        //-----------Детские товары
        data.setGenderChildrenPr(request.getGenderChildrenPr());
        data.setConditionChildrenPr(request.getConditionChildrenPr());

        //-----------Товары для спорта
        data.setSportsKind(request.getSportsKind());
        data.setSportsType(request.getSportsType());
        data.setGenderSports(request.getGenderSports());
        data.setConditionSports(request.getConditionSports());

        //---------Продукты питания------
        data.setFoodKind(request.getFoodKind());

        //------Здоровье и красота
        data.setHealthKind(request.getHealthKind());
        data.setHealthType(request.getHealthType());
        data.setGenderHealth(request.getGenderHealth());

        //--Флора и фауна--
        data.setAnimalsKind(request.getAnimalsKind());

        return data;
    }
}