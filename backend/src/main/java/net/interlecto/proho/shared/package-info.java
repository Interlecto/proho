/**
 * Shared kernel: value types used by every module (identifiers, money, dates),
 * and the tenant context behind row-level security. Depends on no module.
 */
@ApplicationModule(displayName = "Shared kernel", allowedDependencies = {})
package net.interlecto.proho.shared;

import org.springframework.modulith.ApplicationModule;
