package jobforsite.sitejob.module.shared.mapper;

import jobforsite.sitejob.module.shared.dto.product.PostUpdateRequestDTO;
import jobforsite.sitejob.module.shared.dto.PostUpdateData;
import org.springframework.stereotype.Component;

@Component
public class PostUpdateRequestMapper {

        /**
         * Конвертирует PostUpdateRequestDTO в PostUpdateData для использования в сервисе.
         */
public PostUpdateData toUpdateData(PostUpdateRequestDTO request) {
        PostUpdateData data = new PostUpdateData();

        // Общие поля
        data.setTitle(request.getTitle());
        data.setPrice(request.getPrice());
        data.setFullText(request.getFullText());
        data.setCategory(request.getCategory());
        data.setLocation(request.getLocation());

        // Поля недвижимости
        data.setPropertyType(request.getPropertyType());
        data.setRoomsCount(request.getRoomsCount());
        data.setFloorsApartment(request.getFloorsApartment());
        data.setFloorsCount(request.getFloorsCount());
        data.setFloorsCountHouse(request.getFloorsCountHouse());
        data.setHasElevator(request.getHasElevator());
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

        // Год постройки - определяем по типу
        if ("apartment".equals(request.getPropertyType())) {
        data.setBuildYear(request.getApartmentBuildYear());
        } else if ("house".equals(request.getPropertyType())) {
        data.setBuildYear(request.getHouseBuildYear());
        } else {
        data.setBuildYear(null);
        }

        // Поля автотранспорта
        data.setVehicleType(request.getVehicleType());
        data.setVehicleOrigin(request.getVehicleOrigin());
        data.setVehicleYear(request.getVehicleYear());
        data.setMileage(request.getMileage());
        data.setNumberOfOwners(request.getNumberOfOwners());

        //автозапчасти
        data.setAutoType(request.getAutoType());
        data.setConditionAuto(request.getConditionAuto());

        //Строительные материалы
        data.setConstructionsType(request.getConstructionsType());

        //Дом и интерьер
        data.setInteriorType(request.getInteriorType());
        data.setConditionInterior(request.getConditionInterior());

        //Инструменты
        data.setToolsType(request.getToolsType());
        data.setConditionTools(request.getConditionTools());

        //Бытовая техника
        data.setAppliancesType(request.getAppliancesType());
        data.setConditionAppliances(request.getConditionAppliances());

        //Электроника
        data.setElectronicsType(request.getElectronicsType());
        data.setConditionElectronics(request.getConditionElectronics());

        //Одежда
        data.setClothingType(request.getClothingType());
        data.setGenderClothing(request.getGenderClothing());
        data.setConditionClothing(request.getConditionClothing());

        //Детские товары
        data.setGenderChildrenPr(request.getGenderChildrenPr());
        data.setConditionChildrenPr(request.getConditionChildrenPr());

        //Спорт
        data.setSportsKind(request.getSportsKind());
        data.setSportsType(request.getSportsType());
        data.setGenderSports(request.getGenderSports());
        data.setConditionSports(request.getConditionSports());

        //Еда
        data.setFoodKind(request.getFoodKind());

        //Здоровье
        data.setHealthKind(request.getHealthKind());
        data.setHealthType(request.getHealthType());
        data.setGenderHealth(request.getGenderHealth());

        //Животные
        data.setAnimalsKind(request.getAnimalsKind());


        // Админские флаги
        data.setPremium(request.getPremium());
        data.setPremiumPriority(request.getPremiumPriority());
        data.setDonate(request.getDonate());

        // Для удаления изображений
        data.setDeleteImageIds(request.getDeleteImageIds());

        return data;
        }
        }