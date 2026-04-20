package com.artemis;
//making a test class
public class Main{
  public static void main(String[] args){
    System.out.println("Artemis is alive.");
        PhysicsEngine physicsEngine = new PhysicsEngine();
        Launcher launcher = new Launcher(0, 0, 45, 100);
        Projectile projectile = new Projectile(10, 5, 0.47, 0.01) {
            @Override
            public void onImpact(){
                System.out.println("Impact! Blast radius: " + blastRadius + " metres");
            }

            @Override
            public String getDisplayName(){
                return "Standard Shell";
            }
        };
        projectile.setPosition(launcher.getPositionX(), launcher.getPositionY());
        projectile.setVelocity(
                launcher.getInitialVelocity() * Math.cos(Math.toRadians(launcher.getAngle())),
                launcher.getInitialVelocity() * Math.sin(Math.toRadians(launcher.getAngle())));
        while (!projectile.hasLanded()) {
            System.out.printf("Projectile at (%.2f, %.2f) with velocity (%.2f, %.2f)%n",
                    projectile.getX(), projectile.getY(), projectile.getVx(), projectile.getVy());
            physicsEngine.update(projectile, 0.1);
        }
        System.out.printf("Final position: (%.2f, %.2f)%n", projectile.getX(), projectile.getY());
  }
}
