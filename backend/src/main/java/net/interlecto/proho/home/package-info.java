/**
 * Home: the member's dashboard. Owns no entities; it reads the other modules
 * to give the three clients one summary of a community, in one call.
 * Later modules (billing, communications) add their part here.
 */
@ApplicationModule(displayName = "Home: member dashboard", allowedDependencies = {"identity", "communities", "properties", "residents"})
package net.interlecto.proho.home;

import org.springframework.modulith.ApplicationModule;
