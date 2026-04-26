import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
public class CraterRenderer{
    private final double pad_left, pad_top, plot_w, plot_h;
    private double max_range, max_height;
    public CraterRenderer(double pad_left, double pad_top, double plot_w, double plot_h, double max_range, double max_height){
        this.pad_left =pad_left;
        this.pad_top =pad_top;
        this.plot_w =plot_w;
        this.plot_h =plot_h;
        this.max_range =max_range;
        this.max_height =max_height;
    }
    public void updateBounds(double max_range, double max_height){
        this.max_range =max_range;
        this.max_height =max_height;
    }
    public void render(GraphicsContext gc, CraterManager manager){
        for(Crater c : manager.getCraters()){
            drawCrater(gc, c);
        }
    }
    private void drawCrater(GraphicsContext gc, Crater c){
        double cx =toCanvasX(c.getX());
        double ground_y =toCanvasY(0);
        double radius_px =(c.getWidth() / max_range) * plot_w;
        gc.setStroke(Color.web("#ff6600"));
        gc.setLineWidth(3);
        gc.strokeLine(cx - radius_px, ground_y, cx + radius_px, ground_y);
    }
    private double toCanvasX(double world_x){
        return pad_left + (world_x / max_range) * plot_w;
    }
    private double toCanvasY(double world_y){
        return pad_top + plot_h - (world_y / max_height) * plot_h;
    }
}