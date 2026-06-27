package com.fih.companion.verification.admin;

import com.fih.companion.domain.Voucher;
import com.fih.companion.verification.projection.VoucherSearchProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Backoffice "Vérification Voucher" search (3.2). Separate, read-only repository
 * so VoucherRepository is left untouched.
 *
 * SCALE (built for ~100 000 rows): same approach as the billet search — lookups
 * only on indexed columns (codebarre unique index voucher_codebarre_key +
 * indexcodebarreonline; numeroserie PK), interface projection limited to the
 * displayed columns, COUNT on `voucher` alone (the LEFT JOINs enrich, never
 * filter), server-side pagination. Exact = index seek; prefix = index-eligible
 * under C.UTF-8; no "contains".
 */
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

    @Query(value = SELECT + " WHERE v.codebarre = :value ORDER BY v.numeroserie",
            countQuery = "SELECT count(*) FROM voucher v WHERE v.codebarre = :value",
            nativeQuery = true)
    Page<VoucherSearchProjection> searchByCodebarre(@Param("value") String value, Pageable pageable);

    @Query(value = SELECT + " WHERE v.codebarre LIKE :prefix ESCAPE '\\' ORDER BY v.codebarre, v.numeroserie",
            countQuery = "SELECT count(*) FROM voucher v WHERE v.codebarre LIKE :prefix ESCAPE '\\'",
            nativeQuery = true)
    Page<VoucherSearchProjection> searchByCodebarrePrefix(@Param("prefix") String prefix, Pageable pageable);

    @Query(value = SELECT + " WHERE v.numeroserie = :value ORDER BY v.numeroserie",
            countQuery = "SELECT count(*) FROM voucher v WHERE v.numeroserie = :value",
            nativeQuery = true)
    Page<VoucherSearchProjection> searchByNumeroserie(@Param("value") String value, Pageable pageable);

    @Query(value = SELECT + " WHERE v.numeroserie LIKE :prefix ESCAPE '\\' ORDER BY v.numeroserie",
            countQuery = "SELECT count(*) FROM voucher v WHERE v.numeroserie LIKE :prefix ESCAPE '\\'",
            nativeQuery = true)
    Page<VoucherSearchProjection> searchByNumeroseriePrefix(@Param("prefix") String prefix, Pageable pageable);
}
