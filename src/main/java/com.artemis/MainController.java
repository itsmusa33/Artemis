import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class MainController{

    private static final double CANVAS_WIDTH         = 1100;
    private static final double CANVAS_HEIGHT        = 560;
    private static final double PADDING_LEFT         = 60;
    private static final double PADDING_RIGHT        = 20;
    private static final double PADDING_TOP          = 20;
    private static final double PADDING_BOTTOM       = 50;
    private static final double PLOT_WIDTH           = CANVAS_WIDTH  - PADDING_LEFT - PADDING_RIGHT;
    private static final double PLOT_HEIGHT          = CANVAS_HEIGHT - PADDING_TOP  - PADDING_BOTTOM;
    private static final double HINT_FRACTION        = 0.18;
    private static final double TARGET_SPEED_MIN     = 4.0;
    private static final double TARGET_SPEED_MAX     = 12.0;
    private static final String SOUND_LAUNCH_PATH    = "/sounds/launchsound.mp3";
    private static final String SOUND_IMPACT_PATH    = "/sounds/impactsound.mp3";

    //sidebar
    @FXML private Slider angle_slider;
    @FXML private Label  angle_value_label;
    @FXML private Label  velocity_value_label;
    @FXML private Label  weapon_name_label;
    @FXML private Button custom_weapon_button;
    @FXML private Slider custom_velocity_slider;
    @FXML private Label  custom_velocity_label;
    @FXML private VBox   custom_velocity_box;
    @FXML private Label  ammo_loaded_label;
    @FXML private Slider gravity_slider;
    @FXML private Label  gravity_value_label;
    @FXML private Slider wind_slider;
    @FXML private Label  wind_value_label;
    @FXML private Button trajectory_toggle_button;

    //sim toolbar
    @FXML private Button sim_mode_button;
    @FXML private Button place_targets_button;
    @FXML private Button challenge_static_button;
    @FXML private Button challenge_moving_button;
    @FXML private Button auto_aim_button;
    @FXML private Button clear_targets_button;
    @FXML private Button clear_craters_button;

    //stats bar
    @FXML private Label  range_stat_label;
    @FXML private Label  height_stat_label;
    @FXML private Label  time_stat_label;
    @FXML private Label  height_box_title_label;
    @FXML private Button launch_button;

    @FXML private StackPane canvas_pane;
    private Canvas drawing_canvas;

    private MediaPlayer sound_launch_player;
    private MediaPlayer sound_impact_player;
    private MediaPlayer sound_background_player;

    private Launcher      active_launcher;
    private Projectile    active_projectile;
    private BasicPhysics  physics_engine;
    private Artillery     active_weapon;
    private AnimationTimer flight_animation_timer;
    private SimMode       current_sim_mode       = SimMode.NONE;
    private boolean       is_animating           = false;
    private boolean       show_trajectory        = true;
    private boolean       is_placing_targets     = false;
    private boolean       is_custom_weapon       = false;
    private boolean       is_auto_aim_on         = false;
    private int           hit_count              = 0;
    private int           miss_count             = 0;
    private int           breach_count           = 0;
    private boolean       suppress_angle_event   = false;
    private long          out_of_range_expiry_nanos = 0;
    private double        out_of_range_screen_x     = 0;
    private long          last_frame_nanos       = 0;
    private double        current_flight_time    = 0.0;
    private double        peak_height_reached    = 0.0;
    private double        max_plot_range, max_plot_height;

    private int           countdown_value        = 0;
    private boolean       countdown_running      = false;
    private Timeline      countdown_timeline;

    private TargetManager  target_manager;
    private TargetRenderer target_renderer;
    private CraterManager  crater_manager;
    private CraterRenderer crater_renderer;
    private AnimationTimer idle_movement_timer;

    // -------------------------------------------------------------------------

    private MediaPlayer load_sound_from_path(String path){
        try{
            var url = getClass().getResource(path);
            if(url == null){ System.err.println("missing sound: " + path); return null; }
            return new MediaPlayer(new Media(url.toExternalForm()));
        } catch(Exception e){
            System.err.println("sound load failed: " + e.getMessage());
            return null;
        }
    }

    private void play_sound(MediaPlayer p){
        if(p == null) return;
        p.stop();
        p.seek(Duration.ZERO);
        p.play();
    }

    // -------------------------------------------------------------------------

    @FXML
    public void initialize(){
        physics_engine = new BasicPhysics();
        active_weapon  = new Mortar();
        active_weapon.loadAmmo(new Shell50mm());
        target_manager = new TargetManager();
        crater_manager = new CraterManager();

        sound_background_player = load_sound_from_path("/sounds/backgroundmusic.mp3");
        if(sound_background_player != null){
            sound_background_player.setCycleCount(MediaPlayer.INDEFINITE);
            sound_background_player.setVolume(0.4);
            sound_background_player.play();
        }

        // Register listeners LAST — all @FXML fields are injected by this point
        angle_slider.valueProperty().addListener((obs, o, n) -> {
            if(!suppress_angle_event && is_animating) stop_animation();
            angle_value_label.setText(String.format("%.0f°", n.doubleValue()));
            if(!suppress_angle_event){ update_stats_bar(); redraw_canvas(); }
        });

        gravity_slider.valueProperty().addListener((obs, o, n) -> {
            if(is_animating) stop_animation();
            gravity_value_label.setText(String.format("%.1f m/s²", n.doubleValue()));
            recalc_plot_bounds();
            update_stats_bar();
            redraw_canvas();
        });

        wind_slider.valueProperty().addListener((obs, o, n) -> {
            if(is_animating) stop_animation();
            double w = n.doubleValue();
            String dir;
            if      (w >  0.05) dir = " →";
            else if (w < -0.05) dir = " ←";
            else                dir = "";
            wind_value_label.setText(String.format("%.1f m/s%s", Math.abs(w), dir));
            update_stats_bar();
            redraw_canvas();
        });

        custom_velocity_slider.valueProperty().addListener((obs, o, n) -> {
            if(!is_custom_weapon) return;
            custom_velocity_label.setText(String.format("%.0f m/s", n.doubleValue()));
            velocity_value_label.setText(String.format("%.0f m/s", n.doubleValue()));
            active_weapon = new Artillery("Custom", n.doubleValue()){};
            update_stats_bar();
            redraw_canvas();
        });
    }

    //called after show() so the pane has its actual size
    public void init_canvas(){
        drawing_canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        canvas_pane.getChildren().add(drawing_canvas);

        sound_launch_player = load_sound_from_path(SOUND_LAUNCH_PATH);
        sound_impact_player = load_sound_from_path(SOUND_IMPACT_PATH);

        recalc_plot_bounds();
        target_renderer = new TargetRenderer(PADDING_LEFT, PADDING_TOP, PLOT_WIDTH, PLOT_HEIGHT, max_plot_range, max_plot_height);
        crater_renderer = new CraterRenderer(PADDING_LEFT, PADDING_TOP, PLOT_WIDTH, PLOT_HEIGHT, max_plot_range, max_plot_height);

        drawing_canvas.setOnMouseClicked(e -> {
            if(!is_placing_targets || current_sim_mode == SimMode.NONE) return;
            double world_x   = (e.getX() - PADDING_LEFT) / PLOT_WIDTH * max_plot_range;
            double v         = active_weapon.GetInitialVelocity();
            double g         = gravity_slider.getValue();
            double ballistic_max = (v * v) / g;
            if(world_x >= 0 && world_x <= ballistic_max){
                target_manager.addTarget(world_x);
                if(is_auto_aim_on) compute_and_apply_auto_aim();
                redraw_canvas();
            } else if(world_x > ballistic_max){
                show_out_of_range_warning(world_x);
            }
        });

        //A/D nudge angle, M fires
        canvas_pane.getScene().setOnKeyPressed(ke -> {
            KeyCode k = ke.getCode();
            if      (k == KeyCode.A) angle_slider.setValue(Math.max(1,  angle_slider.getValue() - 1));
            else if (k == KeyCode.D) angle_slider.setValue(Math.min(89, angle_slider.getValue() + 1));
            else if (k == KeyCode.M){ if(is_animating) stop_animation(); else start_animation(); }
        });

        ammo_loaded_label.setText("50 mm");
        update_stats_bar();
        redraw_canvas();
    }

    // -------------------------------------------------------------------------

    @FXML private void onMortarBtn()       { set_active_weapon(new Mortar()); }
    @FXML private void onFieldGunBtn()     { set_active_weapon(new FieldGun()); }
    @FXML private void onMissileBtn()      { set_active_weapon(new MissileSystem()); }
    @FXML private void onCustomWeaponBtn() { toggle_custom_weapon(); }

    @FXML private void onShell50()   { set_active_ammo(new Shell50mm()); }
    @FXML private void onShell100()  { set_active_ammo(new Shell100mm()); }
    @FXML private void onShell150()  { set_active_ammo(new Shell150mm()); }
    @FXML private void onClearAmmo() { set_active_ammo(null); }

    @FXML private void onEarthPreset(){ gravity_slider.setValue(9.81); }
    @FXML private void onMoonPreset() { gravity_slider.setValue(1.62); }

    @FXML private void onTrajectoryToggle(){
        show_trajectory = !show_trajectory;
        if(show_trajectory){
            trajectory_toggle_button.setText("  Trajectory ON");
            trajectory_toggle_button.setStyle(style_button_on());
        } else{
            trajectory_toggle_button.setText("  Trajectory OFF");
            trajectory_toggle_button.setStyle(style_button_off());
        }
        redraw_canvas();
    }

    // -------------------------------------------------------------------------

    @FXML private void onSimModeToggle(){
        if(current_sim_mode != SimMode.NONE) exit_sim_mode();
        else                                 enter_sim_mode();
    }

    @FXML private void onPlaceTargets(){
        if(current_sim_mode == SimMode.NONE) return;
        is_placing_targets = !is_placing_targets;
        if(is_placing_targets){
            place_targets_button.setStyle(style_button_on());
        } else{
            place_targets_button.setStyle(style_button_off());
        }
    }

    @FXML private void onChallengeStatic(){
        if(current_sim_mode == SimMode.NONE) return;
        if(current_sim_mode == SimMode.CHALLENGE_STATIC){
            stop_idle_movement_timer();
            stop_countdown();
            target_manager.clearTargets();
            current_sim_mode = SimMode.PRACTICE;
            challenge_static_button.setStyle(style_button_off());
            show_trajectory = true;
            trajectory_toggle_button.setText("  Trajectory ON");
            trajectory_toggle_button.setStyle(style_button_on());
            hit_count = miss_count = breach_count = 0;
            redraw_canvas();
            return;
        }
        stop_idle_movement_timer();
        target_manager.clearTargets();
        current_sim_mode = SimMode.CHALLENGE_STATIC;
        challenge_static_button.setStyle(style_button_on());
        challenge_moving_button.setStyle(style_button_off());
        hit_count = miss_count = breach_count = 0;
        start_countdown(() -> {
            recalc_plot_bounds();
            double ballistic_max = (active_weapon.GetInitialVelocity() * active_weapon.GetInitialVelocity()) / gravity_slider.getValue();
            target_manager.generateRandomTargets(1 + new java.util.Random().nextInt(4), ballistic_max);
            show_trajectory = false;
            trajectory_toggle_button.setText("  Trajectory OFF");
            trajectory_toggle_button.setStyle(style_button_off());
            if(is_auto_aim_on) compute_and_apply_auto_aim();
            redraw_canvas();
        });
    }

    @FXML private void onChallengeMoving(){
        if(current_sim_mode == SimMode.NONE) return;
        if(current_sim_mode == SimMode.CHALLENGE_MOVING){
            stop_idle_movement_timer();
            stop_countdown();
            target_manager.clearTargets();
            current_sim_mode = SimMode.PRACTICE;
            challenge_moving_button.setStyle(style_button_off());
            show_trajectory = true;
            trajectory_toggle_button.setText("  Trajectory ON");
            trajectory_toggle_button.setStyle(style_button_on());
            hit_count = miss_count = breach_count = 0;
            redraw_canvas();
            return;
        }
        stop_idle_movement_timer();
        target_manager.clearTargets();
        current_sim_mode = SimMode.CHALLENGE_MOVING;
        challenge_moving_button.setStyle(style_button_on());
        challenge_static_button.setStyle(style_button_off());
        hit_count = miss_count = breach_count = 0;
        start_countdown(() -> {
            recalc_plot_bounds();
            target_manager.generateMovingTargets(
                    1 + new java.util.Random().nextInt(4),
                    max_plot_range, TARGET_SPEED_MIN, TARGET_SPEED_MAX);
            show_trajectory = false;
            trajectory_toggle_button.setText("  Trajectory OFF");
            trajectory_toggle_button.setStyle(style_button_off());
            start_idle_movement_timer();
            if(is_auto_aim_on) compute_and_apply_auto_aim();
            redraw_canvas();
        });
    }

    @FXML private void onAutoAim(){
        if(current_sim_mode == SimMode.NONE) return;
        is_auto_aim_on = !is_auto_aim_on;
        if(is_auto_aim_on){
            auto_aim_button.setStyle(style_button_on());
        } else{
            auto_aim_button.setStyle(style_button_off());
        }
        if(is_auto_aim_on) compute_and_apply_auto_aim();
        redraw_canvas();
    }

    @FXML private void onClearTargets(){
        if(current_sim_mode == SimMode.NONE) return;
        stop_idle_movement_timer();
        target_manager.clearTargets();
        redraw_canvas();
    }

    @FXML private void onClearCraters(){
        if(current_sim_mode == SimMode.NONE) return;
        crater_manager.clearCraters();
        redraw_canvas();
    }

    @FXML private void onLaunch(){
        if(is_animating) stop_animation();
        else             start_animation();
    }

    // -------------------------------------------------------------------------

    private void enter_sim_mode(){
        current_sim_mode = SimMode.PRACTICE;
        sim_mode_button.setStyle(style_button_on());
        sync_toolbar_buttons();
        redraw_canvas();
    }

    private void exit_sim_mode(){
        current_sim_mode = SimMode.NONE;
        sim_mode_button.setStyle(style_button_off());
        is_auto_aim_on     = false;
        is_placing_targets = false;
        show_trajectory    = true;
        trajectory_toggle_button.setText("  Trajectory ON");
        trajectory_toggle_button.setStyle(style_button_on());
        auto_aim_button.setStyle(style_button_off());
        challenge_static_button.setStyle(style_button_off());
        challenge_moving_button.setStyle(style_button_off());
        place_targets_button.setStyle(style_button_off());
        stop_idle_movement_timer();
        stop_countdown();
        target_manager.clearTargets();
        hit_count = miss_count = breach_count = 0;
        sync_toolbar_buttons();
        redraw_canvas();
    }

    // -------------------------------------------------------------------------

    private void start_countdown(Runnable done){
        stop_countdown();
        countdown_value   = 3;
        countdown_running = true;
        draw_countdown_number(countdown_value);

        countdown_timeline = new Timeline();
        for(int i = 1; i <= 3; i++){
            final int tick = 3 - i;
            countdown_timeline.getKeyFrames().add(
                    new KeyFrame(Duration.seconds(i), e -> {
                        countdown_value = tick;
                        if(countdown_value > 0) draw_countdown_number(countdown_value);
                    })
            );
        }
        countdown_timeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(3.1), e -> {
                    countdown_running = false;
                    redraw_canvas();
                    done.run();
                })
        );
        countdown_timeline.play();
    }

    private void stop_countdown(){
        if(countdown_timeline != null){ countdown_timeline.stop(); countdown_timeline = null; }
        countdown_running = false;
    }

    private void draw_countdown_number(int n){
        if(drawing_canvas == null) return;
        GraphicsContext gc = drawing_canvas.getGraphicsContext2D();
        clear_canvas_background(gc);
        draw_grid_and_axes(gc);
        crater_renderer.render(gc, crater_manager);
        target_renderer.render(gc, target_manager);
        draw_trajectory_arc(gc);
        double cx = PADDING_LEFT + PLOT_WIDTH  / 2.0;
        double cy = PADDING_TOP  + PLOT_HEIGHT / 2.0;
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 80));
        gc.fillText(String.valueOf(n), cx - 24, cy + 28);
    }

    // -------------------------------------------------------------------------

    private void sync_toolbar_buttons(){
        boolean on = current_sim_mode != SimMode.NONE;
        place_targets_button.setDisable(!on);
        challenge_static_button.setDisable(!on);
        challenge_moving_button.setDisable(!on);
        auto_aim_button.setDisable(!on);
        clear_targets_button.setDisable(!on);
        clear_craters_button.setDisable(!on);

        if(!on){
            place_targets_button.setStyle(style_button_disabled());
            challenge_static_button.setStyle(style_button_disabled());
            challenge_moving_button.setStyle(style_button_disabled());
            auto_aim_button.setStyle(style_button_disabled());
            clear_targets_button.setStyle(style_button_disabled());
            clear_craters_button.setStyle(style_button_disabled());
        } else{
            if(is_placing_targets){
                place_targets_button.setStyle(style_button_on());
            } else{
                place_targets_button.setStyle(style_button_off());
            }
            if(current_sim_mode == SimMode.CHALLENGE_STATIC){
                challenge_static_button.setStyle(style_button_on());
            } else{
                challenge_static_button.setStyle(style_button_off());
            }
            if(current_sim_mode == SimMode.CHALLENGE_MOVING){
                challenge_moving_button.setStyle(style_button_on());
            } else{
                challenge_moving_button.setStyle(style_button_off());
            }
            if(is_auto_aim_on){
                auto_aim_button.setStyle(style_button_on());
            } else{
                auto_aim_button.setStyle(style_button_off());
            }
            clear_targets_button.setStyle(style_button_off());
            clear_craters_button.setStyle(style_button_off());
        }
    }

    // -------------------------------------------------------------------------

    //iterative solve — corrects for wind on each pass
    private double calc_aim_angle_for_static_target(double tx){
        double v  = active_weapon.GetInitialVelocity();
        double g  = gravity_slider.getValue();
        double wa = wind_slider.getValue() * 0.1;
        double ax = tx;
        double rad = 0;
        for(int i = 0; i < 6; i++){
            double s = (g * ax) / (v * v);
            if(s < -1 || s > 1) return -1;
            rad = 0.5 * Math.asin(s);
            double vy = v * Math.sin(rad);
            if(vy <= 0) return -1;
            double tf = (2 * vy) / g;
            ax = tx - 0.5 * wa * tf * tf;
        }
        double deg = Math.toDegrees(rad);
        if(deg > 0.5 && deg < 89.5){
            return deg;
        } else{
            return -1;
        }
    }

    //predicts where the target will be when shell arrives
    private double calc_aim_angle_for_moving_target(Target t){
        double v   = active_weapon.GetInitialVelocity();
        double g   = gravity_slider.getValue();
        double wa  = wind_slider.getValue() * 0.1;
        double spd = t.getSpeed();
        double rad = Math.toRadians(45);
        for(int i = 0; i < 8; i++){
            double vy = v * Math.sin(rad);
            if(vy <= 0){ rad = Math.toRadians(45); vy = v * Math.sin(rad); }
            double tf = (2 * vy) / g;
            double px = t.getX() - spd * tf;
            if(px <= 0) return -1;
            double ax = px - 0.5 * wa * tf * tf;
            double s  = (g * ax) / (v * v);
            if(s < -1 || s > 1) return -1;
            rad = 0.5 * Math.asin(s);
        }
        double deg = Math.toDegrees(rad);
        if(deg > 0.5 && deg < 89.5){
            return deg;
        } else{
            return -1;
        }
    }

    private void compute_and_apply_auto_aim(){
        if(!is_auto_aim_on) return;
        Target t = target_manager.getNearestTarget();
        if(t == null) return;
        double deg;
        if(t.isMoving()){
            deg = calc_aim_angle_for_moving_target(t);
        } else{
            deg = calc_aim_angle_for_static_target(t.getX());
        }
        if(deg > 0){
            suppress_angle_event = true;
            angle_slider.setValue(deg);
            suppress_angle_event = false;
            angle_value_label.setText(String.format("%.0f°", deg));
        }
    }

    // -------------------------------------------------------------------------

    //keeps targets moving while not firing
    private void start_idle_movement_timer(){
        if(idle_movement_timer != null) return;
        last_frame_nanos = System.nanoTime();
        idle_movement_timer = new AnimationTimer(){
            @Override public void handle(long now){
                if(is_animating){ last_frame_nanos = now; return; }
                double dt = Math.min((now - last_frame_nanos) / 1_000_000_000.0, 0.05);
                last_frame_nanos = now;
                breach_count += target_manager.updateMovingTargets(dt);
                if(is_auto_aim_on) compute_and_apply_auto_aim();
                redraw_canvas();
            }
        };
        idle_movement_timer.start();
    }

    private void stop_idle_movement_timer(){
        if(idle_movement_timer != null){ idle_movement_timer.stop(); idle_movement_timer = null; }
    }

    // -------------------------------------------------------------------------

    private void toggle_custom_weapon(){
        is_custom_weapon = !is_custom_weapon;
        if(is_custom_weapon){
            custom_velocity_box.setVisible(true);
            custom_velocity_box.setManaged(true);
            custom_weapon_button.setStyle(style_button_on());
            custom_weapon_button.setText("Custom ON");
            double cv = custom_velocity_slider.getValue();
            active_weapon = new Artillery("Custom", cv){};
            weapon_name_label.setText("Custom");
            velocity_value_label.setText(String.format("%.0f m/s", cv));
        } else{
            custom_velocity_box.setVisible(false);
            custom_velocity_box.setManaged(false);
            custom_weapon_button.setStyle(style_button_off());
            custom_weapon_button.setText("Custom Weapon");
            set_active_weapon(new Mortar());
        }
        update_stats_bar();
        redraw_canvas();
    }

    // -------------------------------------------------------------------------

    private void recalc_plot_bounds(){
        double v = active_weapon.GetInitialVelocity();
        double g;
        if(gravity_slider != null){
            g = gravity_slider.getValue();
        } else{
            g = 9.81;
        }
        max_plot_range  = (v * v) / g * 1.20;
        max_plot_height = (v * v) / (2 * g) * 1.50;
        if(target_renderer != null) target_renderer.updateBounds(max_plot_range, max_plot_height);
        if(crater_renderer != null) crater_renderer.updateBounds(max_plot_range, max_plot_height);
    }

    private double world_to_screen_x(double wx){ return PADDING_LEFT + (wx / max_plot_range)  * PLOT_WIDTH; }
    private double world_to_screen_y(double wy){ return PADDING_TOP  + PLOT_HEIGHT - (wy / max_plot_height) * PLOT_HEIGHT; }

    private void clear_canvas_background(GraphicsContext gc){
        gc.setFill(Color.web(ColorScheme.CANVAS_BG));
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
    }

    private void draw_grid_and_axes(GraphicsContext gc){
        gc.setFill(Color.web(ColorScheme.PLOT_BG));
        gc.fillRect(PADDING_LEFT, PADDING_TOP, PLOT_WIDTH, PLOT_HEIGHT);

        gc.setStroke(Color.web(ColorScheme.GRID_LINES));
        gc.setLineWidth(0.8);
        for(int i = 0; i <= 10; i++){
            double x = PADDING_LEFT + i * PLOT_WIDTH  / 10.0;
            double y = PADDING_TOP  + i * PLOT_HEIGHT / 10.0;
            gc.strokeLine(x, PADDING_TOP, x, PADDING_TOP + PLOT_HEIGHT);
            gc.strokeLine(PADDING_LEFT, y, PADDING_LEFT + PLOT_WIDTH, y);
        }

        gc.setStroke(Color.web(ColorScheme.GRID_AXES));
        gc.setLineWidth(2);
        gc.strokeLine(PADDING_LEFT, PADDING_TOP + PLOT_HEIGHT, PADDING_LEFT + PLOT_WIDTH, PADDING_TOP + PLOT_HEIGHT);
        gc.strokeLine(PADDING_LEFT, PADDING_TOP, PADDING_LEFT, PADDING_TOP + PLOT_HEIGHT);

        gc.setFill(Color.web(ColorScheme.GRID_LABELS));
        gc.setFont(Font.font("Monospace", 11));
        for(int i = 0; i <= 5; i++){
            double px  = PADDING_LEFT + i * PLOT_WIDTH / 5.0;
            double val = i * max_plot_range / 5.0;
            String lbl;
            if(val >= 1000){
                lbl = String.format("%.1fk", val / 1000);
            } else{
                lbl = String.format("%.0f", val);
            }
            gc.fillText(lbl, px - 10, PADDING_TOP + PLOT_HEIGHT + 16);
            double py = PADDING_TOP + PLOT_HEIGHT - i * PLOT_HEIGHT / 5.0;
            double yv = i * max_plot_height / 5.0;
            gc.fillText(String.format("%.0f", yv), 4, py + 4);
        }

        gc.setFill(Color.web(ColorScheme.AXIS_TITLES));
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        gc.fillText("Distance (m)", PADDING_LEFT + PLOT_WIDTH / 2 - 35, CANVAS_HEIGHT - 4);
        gc.save();
        gc.translate(12, PADDING_TOP + PLOT_HEIGHT / 2 + 30);
        gc.rotate(-90);
        gc.fillText("Height (m)", 0, 0);
        gc.restore();

        gc.setFill(Color.web("#1a3d5c"));
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        gc.fillText("ARTEMIS", PADDING_LEFT + PLOT_WIDTH / 2 - 58, PADDING_TOP + 36);

        if(current_sim_mode != SimMode.NONE){
            gc.setFill(Color.web("#ff444488"));
            gc.fillRoundRect(PADDING_LEFT + PLOT_WIDTH - 210, PADDING_TOP + 8, 202, 24, 6, 6);
            gc.setFill(Color.web("#ff6655"));
            gc.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
            String ml;
            if(current_sim_mode == SimMode.PRACTICE){
                ml = "⬤  PRACTICE MODE";
            } else if(current_sim_mode == SimMode.CHALLENGE_STATIC){
                ml = "⬤  CHALLENGE: STATIC";
            } else if(current_sim_mode == SimMode.CHALLENGE_MOVING){
                ml = "⬤  CHALLENGE: MOVING";
            } else{
                ml = "";
            }
            gc.fillText(ml, PADDING_LEFT + PLOT_WIDTH - 204, PADDING_TOP + 24);
        }
    }

    private void draw_trajectory_arc(GraphicsContext gc){
        double rad = Math.toRadians(angle_slider.getValue());
        double v   = active_weapon.GetInitialVelocity();
        double g   = gravity_slider.getValue();
        double wa  = wind_slider.getValue() * 0.1;
        double vx0 = v * Math.cos(rad);
        double vy0 = v * Math.sin(rad);
        if(vy0 <= 0) return;
        double tt = (2 * vy0) / g;

        if(show_trajectory){
            int steps = 300;
            gc.setLineWidth(2);
            for(int i = 1; i <= steps; i++){
                double t1 = (i-1) * tt / steps, t2 = i * tt / steps;
                double x1 = vx0*t1 + 0.5*wa*t1*t1, y1 = vy0*t1 - 0.5*g*t1*t1;
                double x2 = vx0*t2 + 0.5*wa*t2*t2, y2 = vy0*t2 - 0.5*g*t2*t2;
                double f  = (double) i / steps;
                gc.setStroke(Color.rgb(
                        (int)(30 + 20*f), (int)(180 - 80*f), (int)(220 + 35*f), 0.7));
                gc.strokeLine(world_to_screen_x(x1), world_to_screen_y(y1), world_to_screen_x(x2), world_to_screen_y(y2));
            }
        } else{
            double th = tt * HINT_FRACTION;
            gc.setLineWidth(2.2);
            for(int i = 1; i <= 60; i++){
                double t1 = (i-1)*th/60, t2 = i*th/60;
                double x1 = vx0*t1 + 0.5*wa*t1*t1, y1 = vy0*t1 - 0.5*g*t1*t1;
                double x2 = vx0*t2 + 0.5*wa*t2*t2, y2 = vy0*t2 - 0.5*g*t2*t2;
                gc.setStroke(Color.rgb(77, 184, 255, 0.65 * (1.0 - (double)i/60)));
                gc.strokeLine(world_to_screen_x(x1), world_to_screen_y(y1), world_to_screen_x(x2), world_to_screen_y(y2));
            }
        }

        double ox = world_to_screen_x(0), oy = world_to_screen_y(0);
        gc.setStroke(Color.web(ColorScheme.ANGLE_ARC));
        gc.setLineWidth(1);
        gc.strokeArc(ox-22, oy-22, 44, 44, 0, angle_slider.getValue(), ArcType.OPEN);
        gc.setFill(Color.web(ColorScheme.ANGLE_LABEL));
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        gc.fillText(String.format("%.0f°", angle_slider.getValue()), ox + 28, oy - 8);

        if(is_auto_aim_on && !is_animating){
            Target nearest = target_manager.getNearestTarget();
            if(nearest != null){
                double ar  = Math.toRadians(angle_slider.getValue());
                double avx = v * Math.cos(ar);
                double avy = v * Math.sin(ar);
                double tf;
                if(avy > 0){
                    tf = (2 * avy) / g;
                } else{
                    tf = 0;
                }
                double lx = avx * tf + 0.5 * wa * tf * tf;

                gc.setStroke(Color.web("#ffcc44aa"));
                gc.setLineWidth(1);
                gc.setLineDashes(6, 4);
                gc.strokeLine(ox, oy, world_to_screen_x(lx), world_to_screen_y(0));
                gc.setLineDashes(0);

                double crx;
                if(nearest.isMoving()){
                    crx = world_to_screen_x(Math.max(0, nearest.getX() - nearest.getSpeed() * tf));
                } else{
                    crx = world_to_screen_x(nearest.getX());
                }
                double cry = world_to_screen_y(0);
                gc.setStroke(Color.web("#ffcc44"));
                gc.setLineWidth(1.5);
                gc.strokeOval(crx - 10, cry - 10, 20, 20);
                gc.strokeLine(crx - 14, cry, crx + 14, cry);
                gc.strokeLine(crx, cry - 14, crx, cry + 14);
                gc.setFill(Color.web("#ffcc44"));
                gc.setFont(Font.font("Monospace", 9));
                gc.fillText("PRED", crx + 13, cry - 3);
            }
        }
    }

    private void draw_projectile_sprite(GraphicsContext gc, double wx, double wy){
        double px = world_to_screen_x(wx), py = world_to_screen_y(wy);
        double da;
        if(active_projectile != null){
            da = Math.atan2(active_projectile.getVy(), active_projectile.getVx());
        } else{
            da = Math.toRadians(angle_slider.getValue());
        }
        gc.save();
        gc.translate(px, py);
        gc.rotate(-Math.toDegrees(da));
        gc.setFill(Color.web("#66ffaa"));
        gc.fillRoundRect(-8, -2, 16, 5, 3, 3);
        gc.fillPolygon(new double[]{8,  8,  13}, new double[]{-2,  3, 0.5}, 3);
        gc.fillPolygon(new double[]{-8, -8, -12}, new double[]{-2, -5, -2}, 3);
        gc.fillPolygon(new double[]{-8, -8, -12}, new double[]{ 3,  6,  3}, 3);
        gc.restore();
    }

    private void draw_score_hud(GraphicsContext gc){
        if(current_sim_mode == SimMode.NONE) return;
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        gc.setFill(Color.WHITE);
        String extra;
        if(current_sim_mode == SimMode.CHALLENGE_MOVING){
            extra = "   BREACH: " + breach_count;
        } else{
            extra = "";
        }
        String hud = "HIT: " + hit_count + "   MISS: " + miss_count + extra;
        gc.fillText(hud, PADDING_LEFT + 8, PADDING_TOP + 18);
    }

    private void draw_max_range_line(GraphicsContext gc){
        if(current_sim_mode == SimMode.NONE || !is_placing_targets) return;
        double rmax = (active_weapon.GetInitialVelocity() * active_weapon.GetInitialVelocity()) / gravity_slider.getValue();
        double lx   = world_to_screen_x(rmax);
        if(lx < PADDING_LEFT || lx > PADDING_LEFT + PLOT_WIDTH) return;
        gc.save();
        gc.setStroke(Color.web("#ff4444cc"));
        gc.setLineWidth(1.5);
        gc.setLineDashes(8, 5);
        gc.strokeLine(lx, PADDING_TOP, lx, PADDING_TOP + PLOT_HEIGHT);
        gc.setLineDashes(0);
        gc.setFill(Color.web("#ff6666"));
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
        String lbl = String.format("MAX %.0fm", rmax);
        gc.fillText(lbl, lx - lbl.length() * 3.5, PADDING_TOP + 14);
        gc.restore();
    }

    private void draw_out_of_range_warning(GraphicsContext gc){
        if(System.nanoTime() > out_of_range_expiry_nanos) return;
        gc.save();
        gc.setFill(Color.web("#ff2222dd"));
        gc.fillRoundRect(out_of_range_screen_x - 55, PADDING_TOP + PLOT_HEIGHT - 60, 120, 22, 6, 6);
        gc.setFill(Color.web("#ffffff"));
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
        gc.fillText("OUT OF RANGE", out_of_range_screen_x - 48, PADDING_TOP + PLOT_HEIGHT - 44);
        gc.restore();
    }

    private void show_out_of_range_warning(double wx){
        out_of_range_screen_x     = world_to_screen_x(wx);
        out_of_range_expiry_nanos = System.nanoTime() + 600_000_000L;
        redraw_canvas();
        javafx.animation.PauseTransition pt =
                new javafx.animation.PauseTransition(javafx.util.Duration.millis(650));
        pt.setOnFinished(ev -> redraw_canvas());
        pt.play();
    }

    private void redraw_canvas(){
        if(drawing_canvas == null || countdown_running) return;
        recalc_plot_bounds();
        GraphicsContext gc = drawing_canvas.getGraphicsContext2D();
        clear_canvas_background(gc);
        draw_grid_and_axes(gc);
        crater_renderer.render(gc, crater_manager);
        target_renderer.render(gc, target_manager);
        draw_trajectory_arc(gc);
        draw_max_range_line(gc);
        draw_score_hud(gc);
        draw_out_of_range_warning(gc);
    }

    private void draw_animation_frame(double wx, double wy){
        GraphicsContext gc = drawing_canvas.getGraphicsContext2D();
        clear_canvas_background(gc);
        draw_grid_and_axes(gc);
        crater_renderer.render(gc, crater_manager);
        target_renderer.render(gc, target_manager);
        draw_trajectory_arc(gc);
        draw_projectile_sprite(gc, wx, wy);
        draw_max_range_line(gc);
        draw_score_hud(gc);
        draw_out_of_range_warning(gc);
    }

    // -------------------------------------------------------------------------

    private void start_animation(){
        if(flight_animation_timer != null){ flight_animation_timer.stop(); flight_animation_timer = null; }
        if(is_auto_aim_on) compute_and_apply_auto_aim();

        double angle = angle_slider.getValue();
        double wind  = wind_slider.getValue();
        double grav  = gravity_slider.getValue();

        active_launcher = new Launcher(0, 0, angle, active_weapon.GetInitialVelocity());
        active_launcher.setGravity(grav);
        active_launcher.setWind(wind);

        active_projectile = new Projectile(
                active_weapon.GetEffectiveMass(),
                active_weapon.GetEffectiveBlastRadius(),
                0.47){
            @Override public void onImpact(){}
            @Override public String getDisplayName(){ return active_weapon.GetName(); }
        };

        double ar = Math.toRadians(angle);
        active_projectile.setGravity(grav);
        active_projectile.setPosition(0, 0);
        active_projectile.setVelocity(
                active_weapon.GetInitialVelocity() * Math.cos(ar),
                active_weapon.GetInitialVelocity() * Math.sin(ar));

        is_animating         = true;
        current_flight_time  = 0.0;
        peak_height_reached  = 0.0;
        launch_button.setText("  STOP");
        height_box_title_label.setText("HEIGHT");
        play_sound(sound_launch_player);

        stop_idle_movement_timer();
        last_frame_nanos = System.nanoTime();
        final double wa = wind * 0.1;

        flight_animation_timer = new AnimationTimer(){
            @Override public void handle(long now){
                double dt = Math.min((now - last_frame_nanos) / 1_000_000_000.0, 0.05);
                last_frame_nanos = now;

                if(current_sim_mode == SimMode.CHALLENGE_MOVING)
                    breach_count += target_manager.updateMovingTargets(dt);

                if(!active_projectile.hasLanded()){
                    active_projectile.setVx(active_projectile.getVx() + wa * dt);
                    physics_engine.computeStep(active_projectile, dt);
                    double px = active_projectile.getX();
                    double py = active_projectile.getY();
                    if(py > peak_height_reached) peak_height_reached = py;
                    current_flight_time += dt;
                    time_stat_label.setText(String.format("%.2f s", current_flight_time));
                    range_stat_label.setText(String.format("%.1f m", px));
                    height_stat_label.setText(String.format("%.1f m", py));
                    draw_animation_frame(px, py);
                } else{
                    range_stat_label.setText(String.format("%.1f m", active_projectile.getX()));
                    height_stat_label.setText(String.format("%.1f m", peak_height_reached));
                    time_stat_label.setText(String.format("%.2f s", current_flight_time));
                    height_box_title_label.setText("MAX HEIGHT");
                    boolean hit = target_manager.checkImpact(active_projectile.getX(), active_projectile.getBlastRadius());
                    crater_manager.addCrater(active_projectile.getX(), active_projectile);
                    play_sound(sound_impact_player);
                    if(current_sim_mode != SimMode.NONE){
                        if(hit) hit_count++;
                        else    miss_count++;
                    }
                    stop_animation();
                    if(is_auto_aim_on) compute_and_apply_auto_aim();
                    redraw_canvas();
                }
            }
        };
        flight_animation_timer.start();
    }

    private void stop_animation(){
        if(flight_animation_timer != null){ flight_animation_timer.stop(); flight_animation_timer = null; }
        is_animating = false;
        launch_button.setText("  LAUNCH");
        last_frame_nanos = System.nanoTime();
        if(current_sim_mode == SimMode.CHALLENGE_MOVING && !target_manager.getTargets().isEmpty()) start_idle_movement_timer();
    }

    // -------------------------------------------------------------------------

    private void set_active_weapon(Artillery w){
        if(is_animating) stop_animation();
        if(is_custom_weapon){
            is_custom_weapon = false;
            custom_velocity_box.setVisible(false);
            custom_velocity_box.setManaged(false);
            custom_weapon_button.setStyle(style_button_off());
            custom_weapon_button.setText("Custom Weapon");
        }
        active_weapon = w;
        weapon_name_label.setText(w.GetName());
        velocity_value_label.setText(String.format("%.0f m/s", w.GetInitialVelocity()));
        update_stats_bar();
        redraw_canvas();
    }

    private void set_active_ammo(Ammunition ammo){
        active_weapon.loadAmmo(ammo);
        if(ammo != null){
            ammo_loaded_label.setText(ammo.getLabel());
        } else{
            ammo_loaded_label.setText("Default");
        }
        update_stats_bar();
        redraw_canvas();
    }

    private void update_stats_bar(){
            if (angle_slider == null || active_weapon == null) return;
            Launcher temp = new Launcher(0, 0, angle_slider.getValue(), active_weapon.GetInitialVelocity());
            temp.setGravity(gravity_slider.getValue());

            range_stat_label .setText(String.format("%.1f m", physics_engine.getMaxRange(temp)));
            height_stat_label.setText(String.format("%.1f m", physics_engine.getMaxHeight(temp)));
            time_stat_label  .setText(String.format("%.2f s", physics_engine.getFlightTime(temp)));
        }

    private String style_button_on(){
        return "-fx-background-color:#0f3d2e;-fx-text-fill:#66ffaa;" +
                "-fx-font-size:14px;-fx-padding:8 14;-fx-cursor:hand;" +
                "-fx-background-radius:4;-fx-border-color:#66ffaa;" +
                "-fx-border-width:1;-fx-border-radius:4;";
    }

    private String style_button_off(){
        return "-fx-background-color:#1a1a2e;-fx-text-fill:#4a7090;" +
                "-fx-font-size:14px;-fx-padding:8 14;-fx-cursor:hand;" +
                "-fx-background-radius:4;-fx-border-color:#1a3050;" +
                "-fx-border-width:1;-fx-border-radius:4;";
    }

    private String style_button_disabled(){
        return "-fx-background-color:#111122;-fx-text-fill:#283040;" +
                "-fx-font-size:14px;-fx-padding:8 14;-fx-cursor:default;" +
                "-fx-background-radius:4;-fx-border-color:#1a2030;" +
                "-fx-border-width:1;-fx-border-radius:4;";
    }
}