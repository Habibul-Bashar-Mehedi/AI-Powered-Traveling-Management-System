package aptms.repositories.spec;

import aptms.entities.AdminOrder;
import aptms.entities.Product;
import aptms.entities.SystemSetting;
import aptms.entities.User;
import aptms.enums.AdminOrderStatus;
import aptms.enums.UserRole;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public final class AdminSpecifications {

    private AdminSpecifications() {
    }

    public static Specification<User> users(String search, UserRole role) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            
            // Always filter out soft-deleted users
            predicates = cb.and(predicates, cb.isNull(root.get("deletedAt")));
            
            if (search != null && !search.isBlank()) {
                String keyword = "%" + search.trim().toLowerCase() + "%";
                predicates = cb.and(predicates, cb.or(
                        cb.like(cb.lower(root.get("username")), keyword),
                        cb.like(cb.lower(root.get("email")), keyword)
                ));
            }
            if (role != null) {
                predicates = cb.and(predicates, cb.equal(root.get("role"), role));
            }
            return predicates;
        };
    }

    public static Specification<Product> products(String search, Boolean active) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (search != null && !search.isBlank()) {
                String keyword = "%" + search.trim().toLowerCase() + "%";
                predicates = cb.and(predicates, cb.or(
                        cb.like(cb.lower(root.get("name")), keyword),
                        cb.like(cb.lower(root.get("sku")), keyword)
                ));
            }
            if (active != null) {
                predicates = cb.and(predicates, cb.equal(root.get("active"), active));
            }
            return predicates;
        };
    }

    public static Specification<AdminOrder> orders(AdminOrderStatus status, UUID userId, Instant from, Instant to) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (status != null) {
                predicates = cb.and(predicates, cb.equal(root.get("status"), status));
            }
            if (userId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("user").get("id"), userId));
            }
            if (from != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("placedAt"), from));
            }
            if (to != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("placedAt"), to));
            }
            return predicates;
        };
    }

    public static Specification<SystemSetting> settings(String search, String category, Boolean active) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (search != null && !search.isBlank()) {
                String keyword = "%" + search.trim().toLowerCase() + "%";
                predicates = cb.and(predicates, cb.or(
                        cb.like(cb.lower(root.get("settingKey")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword)
                ));
            }
            if (category != null && !category.isBlank()) {
                predicates = cb.and(predicates, cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }
            if (active != null) {
                predicates = cb.and(predicates, cb.equal(root.get("active"), active));
            }
            return predicates;
        };
    }
}
