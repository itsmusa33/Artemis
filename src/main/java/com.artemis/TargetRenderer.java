import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
public class TargetRenderer{
    private final double pad_left, pad_top, plot_w, plot_h;
    private double max_range, max_height;
    public TargetRenderer(double pad_left, double pad_top, double plot_w, double plot_h, double max_range, double max_height){
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
    public void render(GraphicsContext gc, TargetManager manager){
        for(Target t : manager.getTargets()){
            drawTarget(gc, t);
        }
    }
    private void drawTarget(GraphicsContext gc, Target t){
        double cx =toCanvasX(t.getX());
        double tw =toCanvasW(Target.width);
        double th =toCanvasH(Target.height);
        double ground_y =toCanvasY(0);
        // Body
        gc.setFill(Color.web("#3d1a1a"));
        gc.fillRect(cx - tw / 2, ground_y - th, tw, th);
        // Border
        gc.setStroke(Color.web("#ff6655"));
        gc.setLineWidth(1.5);
        gc.strokeRect(cx - tw / 2, ground_y - th, tw, th);
        // Crosshair
        gc.setStroke(Color.web("#ff665588"));
        gc.setLineWidth(0.8);
        gc.strokeLine(cx, ground_y - th, cx, ground_y);
        gc.strokeLine(cx - tw / 2, ground_y - th / 2,
                cx + tw / 2, ground_y - th / 2);
        // Distance label above
        gc.setFill(Color.web("#ff6655"));
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 9));
        gc.fillText(String.format("%.0fm", t.getX()),
                cx - 10, ground_y - th - 4);
    }
    private double toCanvasX(double x){
        return pad_left + (x / max_range) * plot_w;
    }
    private double toCanvasY(double y){
        return pad_top + plot_h - (y / max_height) * plot_h;
    }
    private double toCanvasW(double w){
        return (w / max_range) * plot_w;
    }
    private double toCanvasH(double h){
        return (h / max_height) * plot_h;
    }
}