package lk.sampath_autocare.asset.service_type_parameter_vehicle.dao;

import lk.sampath_autocare.asset.serviceType.entity.ServiceType;
import lk.sampath_autocare.asset.service_type_parameter_vehicle.entity.ServiceTypeParameterVehicle;
import lk.sampath_autocare.asset.service_type_parameter_vehicle.entity.enums.ServiceTypeParameterVehicleStatus;
import lk.sampath_autocare.asset.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ServiceTypeParameterVehicleDao extends JpaRepository< ServiceTypeParameterVehicle, Integer> {
List<ServiceTypeParameterVehicle> findByCreatedAtIsBetween(LocalDateTime form, LocalDateTime to);

  List< ServiceTypeParameterVehicle> findByCreatedAtIsBetweenAndVehicle(LocalDateTime form, LocalDateTime to, Vehicle vehicle);

  List< ServiceTypeParameterVehicle> findByVehicleAndServiceTypeAndServiceTypeParameterVehicleStatus(Vehicle vehicle, ServiceType serviceType, ServiceTypeParameterVehicleStatus serviceTypeParameterVehicleStatus);
}
