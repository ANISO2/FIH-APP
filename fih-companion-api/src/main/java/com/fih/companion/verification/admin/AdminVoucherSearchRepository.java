package com.fih.companion.verification.admin;

import com.fih.companion.domain.Voucher;
import com.fih.companion.verification.projection.VoucherSearchProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface AdminVoucherSearchRepository extends Repository<Voucher, String> {

    String SELECT = """
            SELECT e.titre      AS "eventTitle",
                   m.modele     AS "modelName",
                   v.numeroserie AS "numeroserie",
                   v.codebarre  AS "codebarre",
                   v.utilisation AS "utilisation",
                   v.vendu      AS "vendu",
                   v.activation AS "activation",
                   v.reservation AS "reservation",
                   vo.code      AS "commande"
            FROM voucher v
            LEFT JOIN evenement e     ON e.reference = v.evenement
            LEFT JOIN modelebillet m  ON m.reference = v.modelebillet
            LEFT JOIN voucherorder vo ON vo.reference = v.voucherorder
            """;

    /** Count base with the same evenement join as the select. */
    String COUNT_FROM = "SELECT count(*) FROM voucher v LEFT JOIN evenement e ON e.reference = v.evenement ";


    @Query(value = SELECT + " WHERE v.codebarre = :value " + " ORDER BY v.numeroserie LIMIT :size OFFSET :offset",
            nativeQuery = true)
    List<VoucherSearchProjection> searchByCodebarre(@Param("value") String value,
                                                    @Param("size") int size,
                                                    @Param("offset") int offset);

    @Query(value = COUNT_FROM + " WHERE v.codebarre = :value ", nativeQuery = true)
    long countByCodebarre(@Param("value") String value);

    @Query(value = SELECT + " WHERE v.codebarre LIKE :prefix ESCAPE '\\' " + " ORDER BY v.codebarre, v.numeroserie LIMIT :size OFFSET :offset",
            nativeQuery = true)
    List<VoucherSearchProjection> searchByCodebarrePrefix(@Param("prefix") String prefix,
                                                          @Param("size") int size,
                                                          @Param("offset") int offset);

    @Query(value = COUNT_FROM + " WHERE v.codebarre LIKE :prefix ESCAPE '\\' ", nativeQuery = true)
    long countByCodebarrePrefix(@Param("prefix") String prefix);

    @Query(value = SELECT + " WHERE v.numeroserie = :value " + " ORDER BY v.numeroserie LIMIT :size OFFSET :offset",
            nativeQuery = true)
    List<VoucherSearchProjection> searchByNumeroserie(@Param("value") String value,
                                                      @Param("size") int size,
                                                      @Param("offset") int offset);

    @Query(value = COUNT_FROM + " WHERE v.numeroserie = :value ", nativeQuery = true)
    long countByNumeroserie(@Param("value") String value);

    @Query(value = SELECT + " WHERE v.numeroserie LIKE :prefix ESCAPE '\\' " + " ORDER BY v.numeroserie LIMIT :size OFFSET :offset",
            nativeQuery = true)
    List<VoucherSearchProjection> searchByNumeroseriePrefix(@Param("prefix") String prefix,
                                                            @Param("size") int size,
                                                            @Param("offset") int offset);

    @Query(value = COUNT_FROM + " WHERE v.numeroserie LIKE :prefix ESCAPE '\\' ", nativeQuery = true)
    long countByNumeroseriePrefix(@Param("prefix") String prefix);
}
