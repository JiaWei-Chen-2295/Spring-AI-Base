package com.example.aitemplate.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Controller class or handler method as publicly accessible (no authentication required).
 *
 * <p>Usage:
 * <pre>
 * // Entire controller is public
 * &#64;PublicApi
 * &#64;RestController
 * public class OpenController { ... }
 *
 * // Single method is public
 * &#64;RestController
 * public class ProductController {
 *     &#64;PublicApi
 *     &#64;GetMapping("/featured")
 *     public List&lt;Product&gt; featured() { ... }
 * }
 * </pre>
 *
 * <p>SecurityConfig automatically scans for this annotation at startup and adds
 * the corresponding URL patterns to the permit-all whitelist.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicApi {
}
