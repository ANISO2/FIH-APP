package com.fih.companion.badge;

import com.fih.companion.diagnostics.ConsoleLog;
import com.fih.companion.repository.ModeleBilletRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;


@Service
public class ModelClassificationService {

    private final ModeleBilletRepository modeleRepo;
    private final BadgeProperties badgeProperties;

    private volatile Set<Integer> paidModelCache;

    public ModelClassificationService(ModeleBilletRepository modeleRepo,
                                      BadgeProperties badgeProperties) {
        this.modeleRepo = modeleRepo;
        this.badgeProperties = badgeProperties;
    }

    /** True when the model is sold (vente=true) — excluded from this section. */
    public boolean isPaid(Integer modelId) {
        return modelId != null && paidModels().contains(modelId);
    }

    /** True when the model may be assigned a name here (any non-paid model). */
    public boolean isAffectable(Integer modelId) {
        return modelId != null && !isPaid(modelId);
    }

    /** True when the model may also be printed (configured invitation models). */
    public boolean isPrintable(Integer modelId) {
        return badgeProperties.isInvitationModel(modelId);
    }

    private Set<Integer> paidModels() {
        Set<Integer> cache = this.paidModelCache;
        if (cache == null) {
            synchronized (this) {
                cache = this.paidModelCache;
                if (cache == null) {
                    cache = new HashSet<>(modeleRepo.findPaidModelReferences());
                    this.paidModelCache = cache;
                    ConsoleLog.log("BADGE", "model classification loaded — PAID models (vente=true, excluded "
                            + "from Invitations & Badges) = " + cache
                            + "; PRINTABLE models (fih.badge.invitation-models) = "
                            + badgeProperties.invitationModelSet() + ".");
                }
            }
        }
        return cache;
    }
}
