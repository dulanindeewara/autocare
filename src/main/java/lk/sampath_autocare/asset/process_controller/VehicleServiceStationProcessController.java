package lk.sampath_autocare.asset.process_controller;

import lk.sampath_autocare.asset.common_asset.model.TwoDate;
import lk.sampath_autocare.asset.payment.entity.Payment;
import lk.sampath_autocare.asset.payment.entity.enums.PaymentMethod;
import lk.sampath_autocare.asset.payment.entity.enums.PaymentStatus;
import lk.sampath_autocare.asset.payment.service.PaymentService;
import lk.sampath_autocare.asset.serviceType.entity.ServiceType;
import lk.sampath_autocare.asset.serviceType.service.ServiceTypeService;
import lk.sampath_autocare.asset.service_type_parameter_vehicle.entity.ServiceTypeParameterVehicle;
import lk.sampath_autocare.asset.service_type_parameter_vehicle.entity.enums.ServiceTypeParameterVehicleStatus;
import lk.sampath_autocare.asset.service_type_parameter_vehicle.service.ServiceTypeParameterVehicleService;
import lk.sampath_autocare.asset.vehicle.entity.Vehicle;
import lk.sampath_autocare.asset.vehicle.service.VehicleService;
import lk.sampath_autocare.util.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.ServerSocket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Controller
@RequestMapping( "/vehicleServiceStationProcess" )
public class VehicleServiceStationProcessController {
  private final ServiceTypeParameterVehicleService serviceTypeParameterVehicleService;
  private final DateTimeAgeService dateTimeAgeService;
  private final VehicleService vehicleService;
  private final OperatorService operatorService;
  private final ServiceTypeService serviceTypeService;
  private final PaymentService paymentService;
  private final MakeAutoGenerateNumberService makeAutoGenerateNumberService;
  private final EmailService emailService;
  private final TwilioMessageService twilioMessageService;

  public VehicleServiceStationProcessController(ServiceTypeParameterVehicleService serviceTypeParameterVehicleService
      , DateTimeAgeService dateTimeAgeService, VehicleService vehicleService, PaymentService paymentService,
                                                OperatorService operatorService,
                                                ServiceTypeService serviceTypeService, PaymentService paymentService1,
                                                MakeAutoGenerateNumberService makeAutoGenerateNumberService,
                                                EmailService emailService,
                                                TwilioMessageService twilioMessageService) {
    this.serviceTypeParameterVehicleService = serviceTypeParameterVehicleService;
    this.dateTimeAgeService = dateTimeAgeService;
    this.vehicleService = vehicleService;
    this.operatorService = operatorService;
    this.serviceTypeService = serviceTypeService;
    this.paymentService = paymentService1;
    this.makeAutoGenerateNumberService = makeAutoGenerateNumberService;
    this.emailService = emailService;
    this.twilioMessageService = twilioMessageService;
  }

  @GetMapping
  public String allVehicleOnToday(Model model) {
    model.addAttribute("addStatus", true);
    return common(model, ServiceTypeParameterVehicleStatus.CHK, LocalDate.now(), LocalDate.now());
  }

  private String common(Model model, ServiceTypeParameterVehicleStatus serviceTypeParameterVehicleStatus,
                        LocalDate fromDate, LocalDate toDate) {
    List< Vehicle > vehicles = new ArrayList<>();

    for ( int i = 0; i <= Period.between(fromDate, toDate).getDays(); i++ ) {

      LocalDateTime form = dateTimeAgeService.dateTimeToLocalDateStartInDay(fromDate.plusDays(i));
      LocalDateTime to = dateTimeAgeService.dateTimeToLocalDateEndInDay(fromDate.plusDays(i));
      serviceTypeParameterVehicleService.findByCreatedAtIsBetween(form, to)
          .stream()
          .filter(x -> x.getServiceTypeParameterVehicleStatus().equals(serviceTypeParameterVehicleStatus))
          .collect(Collectors.toList())
          .forEach(x -> {
            Vehicle vehicle = x.getVehicle();
            vehicle.setCreatedAt(form);
            vehicle.setUpdatedAt(to);
            vehicles.add(vehicle);
          });
    }

    if ( ServiceTypeParameterVehicleStatus.DONE.equals(serviceTypeParameterVehicleStatus) ) {
      for ( int k = 0; k < Period.between(fromDate, toDate).getDays(); k++ ) {

        LocalDateTime form = dateTimeAgeService.dateTimeToLocalDateStartInDay(fromDate.plusDays(k));
        LocalDateTime to = dateTimeAgeService.dateTimeToLocalDateEndInDay(fromDate.plusDays(k));
        List< Vehicle > vehiclesAllDone = new ArrayList<>();
        vehicles.forEach(x -> {
          long count = serviceTypeParameterVehicleService.findByCreatedAtIsBetweenAndVehicle(form, to, x)
              .stream()
              .filter(y -> y.getServiceTypeParameterVehicleStatus().equals(ServiceTypeParameterVehicleStatus.CHK) || y.getServiceTypeParameterVehicleStatus().equals(ServiceTypeParameterVehicleStatus.PEND))
              .count();
          if ( count == 0 ) {
            x.setCreatedAt(form);
            x.setUpdatedAt(to);
            vehiclesAllDone.add(x);
          }
        });
        model.addAttribute("vehicles", vehiclesAllDone.stream().distinct().collect(Collectors.toList()));
        return "vehicleServiceStation/vehicleServiceStation";
      }

    }
    model.addAttribute("vehicles", vehicles.stream().distinct().collect(Collectors.toList()));
    return "vehicleServiceStation/vehicleServiceStation";


  }

  @GetMapping( "/done" )
  public String getServiceTypeParameterVehicleStatusDone(Model model) {
    model.addAttribute("addStatus", false);
    model.addAttribute("addStatusPayment", true);
    model.addAttribute("serviceTypeParameterVehicleStatus", ServiceTypeParameterVehicleStatus.DONE);
    model.addAttribute("fromDate", LocalDate.now());
    model.addAttribute("toDate", LocalDate.now());
    return common(model, ServiceTypeParameterVehicleStatus.DONE, LocalDate.now(), LocalDate.now());
  }

  @PostMapping( "/done/search" )
  public String getServiceTypeParameterVehicleStatusDoneSearch(@ModelAttribute TwoDate twoDate, Model model) {
    model.addAttribute("addStatus", false);
    model.addAttribute("addStatusPayment", true);
    model.addAttribute("serviceTypeParameterVehicleStatus", ServiceTypeParameterVehicleStatus.DONE);
    model.addAttribute("fromDate", twoDate.getStartDate());
    model.addAttribute("toDate", twoDate.getEndDate());
    return common(model, ServiceTypeParameterVehicleStatus.DONE, twoDate.getStartDate(), twoDate.getEndDate());
  }

  @GetMapping( "/pending" )
  public String getServiceTypeParameterVehicleStatusPend(Model model) {
    model.addAttribute("addStatus", true);
    return common(model, ServiceTypeParameterVehicleStatus.PEND, LocalDate.now(), LocalDate.now());
  }

  @PostMapping( "/vehicle" )
  public String finishedServiceParameter(@ModelAttribute Vehicle vehicleF, Model model) {
    LocalDateTime form = dateTimeAgeService.dateTimeToLocalDateStartInDay(vehicleF.getForm());
    LocalDateTime to = dateTimeAgeService.dateTimeToLocalDateEndInDay(vehicleF.getTo());

    Vehicle vehicle = vehicleService.findById(vehicleF.getId());
    vehicle.setServiceTypeParameterVehicles(serviceTypeParameterVehicleService
                                                .findByCreatedAtIsBetweenAndVehicle(form, to, vehicle)
                                                .stream()
                                                .filter(x -> !x.getServiceTypeParameterVehicleStatus().equals(ServiceTypeParameterVehicleStatus.DONE))
                                                .collect(Collectors.toList()));

    model.addAttribute("vehicleDetail", vehicle);
    model.addAttribute("vehicle", vehicle);
    List< ServiceTypeParameterVehicleStatus > serviceTypeParameterVehicleStatuses = new ArrayList<>();
    serviceTypeParameterVehicleStatuses.add(ServiceTypeParameterVehicleStatus.CHK);
    serviceTypeParameterVehicleStatuses.add(ServiceTypeParameterVehicleStatus.DONE);
    serviceTypeParameterVehicleStatuses.add(ServiceTypeParameterVehicleStatus.PEND);
    model.addAttribute("serviceTypeParameterVehicleStatuses", serviceTypeParameterVehicleStatuses);
    model.addAttribute("addStatus", true);
    return "vehicleServiceStation/addVehicleServiceStation";
  }

  @PostMapping( "/save" )
  public String save(@ModelAttribute( "vehicle" ) Vehicle vehicle) {
    Vehicle vehicleDB = vehicleService.findById(vehicle.getId());
    vehicle.getServiceTypeParameterVehicles().forEach(serviceTypeParameterVehicleService::persist);

    ServiceType serviceType = serviceTypeService.findById(vehicle.getServiceType().getId());
    List< ServiceTypeParameterVehicle > serviceTypeParameterVehicles = serviceTypeParameterVehicleService
        .findByCreatedAtIsBetweenAndVehicle(dateTimeAgeService.dateTimeToLocalDateStartInDay(LocalDate.now()),
                                            dateTimeAgeService.dateTimeToLocalDateEndInDay(LocalDate.now()), vehicle)
        .stream()
        .filter(x -> x.getServiceType().equals(serviceType))
        .collect(Collectors.toList());

    int allParameterSize = serviceTypeParameterVehicles.size();
    List< ServiceTypeParameterVehicle > jobDoneSize = serviceTypeParameterVehicles
        .stream()
        .filter(x -> x.getServiceTypeParameterVehicleStatus().equals(ServiceTypeParameterVehicleStatus.DONE))
        .collect(Collectors.toList());

    if ( allParameterSize != jobDoneSize.size() ) {
      serviceTypeParameterVehicles
          .stream()
          .filter(x -> x.getServiceTypeParameterVehicleStatus().equals(ServiceTypeParameterVehicleStatus.CHK))
          .collect(Collectors.toList())
          .forEach(x -> {
            x.setServiceTypeParameterVehicleStatus(ServiceTypeParameterVehicleStatus.PEND);
            serviceTypeParameterVehicleService.persist(x);
          });
    }

    if ( allParameterSize == jobDoneSize.size() && serviceType != null ) {

      Payment payment = new Payment();
      payment.setVehicle(vehicleDB);
      payment.setAmount(serviceType.getPrice());
      payment.setTotalAmount(BigDecimal.ZERO);
      payment.setDiscountAmount(BigDecimal.ZERO);
      payment.setAmountTendered(BigDecimal.ZERO);
      payment.setBalance(BigDecimal.ZERO);
      payment.setCustomer(vehicleDB.getCustomer());
      payment.setServiceType(serviceType);
      payment.setPaymentMethod(PaymentMethod.CASH);
      payment.setPaymentStatus(PaymentStatus.NOTPAID);

      Payment lastPayment = paymentService.lastPayment();

      if ( lastPayment == null ) {
        payment.setCode("SAP" + makeAutoGenerateNumberService.numberAutoGen(null).toString());
      } else {
        String previousCode = lastPayment.getCode().substring(3);
        payment.setCode("SAP" + makeAutoGenerateNumberService.numberAutoGen(previousCode).toString());
      }
      Payment paymentSave = paymentService.persist(payment);
      if ( paymentSave.getVehicle().getCustomer().getEmail() != null ) {
        String message = "Your Vehicle's Service is completed.\n" +
            "Invoice Value \t\t\t\t (Rs.) " + paymentSave.getAmount() + "\n" +
            "\n\n" +
            "\t\t Thank you and come again :)\n" +
            "Sampath Auto care\n" +
            "All ways care your vehicle";
        emailService.sendEmail(paymentSave.getVehicle().getCustomer().getEmail(), "Payment Information", message);
      }
      //twilioMessageService.sendSMS();
      /*if ( paymentSave.getVehicle().getCustomer().getMobile() != null ) {
        try {
          String mobileNumber = paymentSave.getVehicle().getCustomer().getMobile().substring(1, 10);
          twilioMessageService.sendSMS("+94" + mobileNumber, "There is immediate PO from " +
                  "Sampath Autocare \nPlease Check Your Email Form Further Details");
        } catch (Exception e) {
          e.printStackTrace();
        }
      }*/


    }


    return "redirect:/vehicleServiceStationProcess";
  }

  @GetMapping( "/paid" )
  public String getServiceTypeParameterVehicleStatusPaid(Model model) {
    model.addAttribute("addStatus", false);
    model.addAttribute("addStatusPayment", true);
    return common(model, ServiceTypeParameterVehicleStatus.PAID, LocalDate.now(), LocalDate.now());
  }

  @PostMapping( "/paid/search" )
  public String getServiceTypeParameterVehicleStatusPaidSearch(@ModelAttribute TwoDate twoDate, Model model) {
    model.addAttribute("addStatus", false);
    model.addAttribute("addStatusPayment", true);
    return common(model, ServiceTypeParameterVehicleStatus.PAID, twoDate.getStartDate(), twoDate.getEndDate());
  }


  @PostMapping( "/printView" )
  public String serviceDetailsPrint(@ModelAttribute Vehicle vehicleF, Model model) {
    LocalDateTime form = dateTimeAgeService.dateTimeToLocalDateStartInDay(vehicleF.getForm());
    LocalDateTime to = dateTimeAgeService.dateTimeToLocalDateEndInDay(vehicleF.getTo());

    Vehicle vehicle = vehicleService.findById(vehicleF.getId());
    vehicle.setServiceTypeParameterVehicles(serviceTypeParameterVehicleService
                                                .findByCreatedAtIsBetweenAndVehicle(form, to, vehicle));

    model.addAttribute("vehicleDetail", vehicle);
    model.addAttribute("vehicle", vehicle);

    return "vehicleServiceStation/vehicleServiceStation-detail";
  }


}
