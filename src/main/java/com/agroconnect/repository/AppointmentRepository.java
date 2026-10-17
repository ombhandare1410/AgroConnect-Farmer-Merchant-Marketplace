package com.agroconnect.repository;

import com.agroconnect.entity.Appointment;
import com.agroconnect.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("select a from Appointment a join fetch a.farmer join fetch a.merchant " +
           "where a.farmer.id = :uid or a.merchant.id = :uid " +
           "order by a.appointmentDate asc, a.appointmentTime asc")
    List<Appointment> findForUser(@Param("uid") Long userId);

    boolean existsByFarmerIdAndMerchantIdAndStatus(Long farmerId, Long merchantId, AppointmentStatus status);

    boolean existsByFarmerIdAndMerchantIdAndStatusIn(Long farmerId, Long merchantId, Collection<AppointmentStatus> statuses);
}
