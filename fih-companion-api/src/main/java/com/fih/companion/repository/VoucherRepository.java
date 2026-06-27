package com.fih.companion.repository;

import com.fih.companion.domain.Voucher;
import com.fih.companion.verification.projection.AccessLogProjection;
import com.fih.companion.verification.projection.VoucherDetailsProjection;
import com.fih.companion.verification.projection.VoucherVerifyProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, String> {

    /** Uses the existing DB index on codebarre. */
    Optional<Voucher> findByCodebarre(String codebarre);

    Optional<Voucher> findByNumeroserie(String numeroserie);

    /**
     * SPIKE FIX — one query for a full voucher verification. Lookup by codebarre
     * OR numeroserie (both indexed), join model/event and our badge_affectation.
     * Live state (utilisation, accesscounter, dateannulation) selected raw; the
     * verdict is decided in Java on every call, never cached.
     */
    @Query(value = """
            SELECT v.numeroserie    AS "numeroserie",
                   v.codebarre      AS "codebarre",
                   v.modelebillet   AS "modelId",
                   e.titre          AS "eventTitle",
                   e.ddate          AS "eventDate",
                   m.modele         AS "modelName",
                   m.maxaccess      AS "maxAccess",
                   v.activation     AS "activation",
                   v.utilisation    AS "utilisation",
                   v.vendu          AS "vendu",
                   v.reservation    AS "reservation",
                   v.accesscounter  AS "accesscounter",
                   v.dateannulation AS "dateannulation",
                   ba.affectee_a    AS "affecteeA"
            FROM voucher v
            LEFT JOIN modelebillet m      ON m.reference   = v.modelebillet
            LEFT JOIN evenement e         ON e.reference   = v.evenement
            LEFT JOIN badge_affectation ba ON ba.numeroserie = v.numeroserie
            WHERE v.codebarre = :code OR v.numeroserie = :code
            ORDER BY (v.codebarre = :code) DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<VoucherVerifyProjection> findForVerification(@Param("code") String code);

    /**
     * DETAILS (lazy) — voucher management extras for the ℹ screen. Single row by
     * PK; LEFT JOIN voucherorder on its indexed FK. Not on the hot scan path.
     */
    @Query(value = """
            SELECT v.numeroserie  AS "numeroserie",
                   v.codebarre    AS "codebarre",
                   vo.code        AS "commande",
                   v.datevente    AS "dateVente"
            FROM voucher v
            LEFT JOIN voucherorder vo ON vo.reference = v.voucherorder
            WHERE v.numeroserie = :numeroserie
            LIMIT 1
            """, nativeQuery = true)
    Optional<VoucherDetailsProjection> findVoucherDetails(@Param("numeroserie") String numeroserie);

    /**
     * DETAILS (lazy) — Public access log for a voucher (tturnstile.voucher,
     * indexed by ix_tturnstile_fk_tturnstile_voucher). Newest first, capped.
     */
    @Query(value = """
            SELECT t.reference        AS "reference",
                   t.codebarre        AS "codebarre",
                   t.datetransaction  AS "datetransaction",
                   t.heuretransaction AS "heuretransaction",
                   t.porte            AS "porte",
                   t.transactionstate AS "transactionstate"
            FROM tturnstile t
            WHERE t.voucher = :numeroserie
            ORDER BY t.heuretransaction DESC NULLS LAST
            LIMIT 200
            """, nativeQuery = true)
    List<AccessLogProjection> findPublicAccessLog(@Param("numeroserie") String numeroserie);

    /** DETAILS (lazy) — VIP access log for a voucher (vipaccess.voucher, indexed). */
    @Query(value = """
            SELECT t.reference        AS "reference",
                   t.codebarre        AS "codebarre",
                   t.datetransaction  AS "datetransaction",
                   t.heuretransaction AS "heuretransaction",
                   t.porte            AS "porte",
                   t.transactionstate AS "transactionstate"
            FROM vipaccess t
            WHERE t.voucher = :numeroserie
            ORDER BY t.heuretransaction DESC NULLS LAST
            LIMIT 200
            """, nativeQuery = true)
    List<AccessLogProjection> findVipAccessLog(@Param("numeroserie") String numeroserie);
}
