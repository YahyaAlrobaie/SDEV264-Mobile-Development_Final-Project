package com.example.kanbanboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

private const val ARG_TASKLIST_TYPE = "tasklist_type"

private const val TASKLIST_TYPE_TODO = 0
private const val TASKLIST_TYPE_DOING = 1
private const val TASKLIST_TYPE_DONE = 2

class TasklistFragment : Fragment(){
    private lateinit var visibleColorPaletteViewList : List<View>
    lateinit var taskRecyclerView: RecyclerView

    private var tasklistType : Int = -1
    private var adapter : TaskViewAdapter? = TaskViewAdapter(LinkedList<Task>())
    private var colorPaletteIsVisible : Boolean = false
    private var callbacks: Callbacks? = null

    //Callback interface to delegate access functions in MainActivity
    interface Callbacks{
        fun addTaskToViewModel(task: Task, destinationTasklistType: Int)
        fun deleteTaskFromViewModel(tasklistType: Int, adapterPosition: Int)
        fun getTaskListFromViewModel(tasklistType: Int) : LinkedList<Task>
    }

    //Attach context as a Callbacks reference to the callbacks variable when fragment attaches to container
    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    //Detach context (assign to null) when fragment detaches from container
    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    //ItemTouchHelper instance with custom callback, to move task card view positions on hold
    private val itemTouchHelper by lazy{
        val taskItemTouchCallback = object : ItemTouchHelper.SimpleCallback(UP or DOWN, 0){
            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {
                val adapter = recyclerView.adapter as TaskViewAdapter
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition

                adapter.moveTaskView(from, to)
                adapter.notifyItemMoved(from, to)

                return true
            }

            //Make taskview transparent while being moved
            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                if(actionState == ACTION_STATE_DRAG)
                    viewHolder?.itemView?.alpha = 0.7f
            }

            //Make taskview opaque while being
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                viewHolder.itemView.alpha = 1.0f
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { /* Not implemented on purpose. */ }
        }
        ItemTouchHelper(taskItemTouchCallback)
    }

    //Get the fragment arguments, tasklist type to be precise, and assign it to the member of this fragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tasklistType = arguments?.getInt(ARG_TASKLIST_TYPE) as Int
    }

    //Inflate view of fragment, prepare recycler view and it's layout manager, and update the UI
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View?{
        //Inflate the layout of this fragment, get recyclerview ref, set layout manager for recycler view
        val view = inflater.inflate(R.layout.tasklist_fragment_layout, container,false)
        taskRecyclerView = view.findViewById(R.id.task_recycler_view) as RecyclerView
        taskRecyclerView.layoutManager = LinearLayoutManager(context)
        itemTouchHelper.attachToRecyclerView(taskRecyclerView)

        //Fill the recyclerview with data from viewmodel
        updateInterface()

        //Return the created view
        return view
    }

    //Populate recyclerview and set up its adapter
    private fun updateInterface(){
        val tasks = callbacks!!.getTaskListFromViewModel(tasklistType)
        adapter = TaskViewAdapter(tasks)
        taskRecyclerView.adapter = adapter
    }

    //Task viewholder declaration. Includes data binding and view creation procedures
    private inner class TaskViewHolder(view: View): RecyclerView.ViewHolder(view){
        lateinit var task: Task

        val taskEditText : EditText = view.findViewById(R.id.task_edit_text)
        val taskLayout : ConstraintLayout = view.findViewById(R.id.task_layout)

        val deleteButton : ImageButton = view.findViewById(R.id.btn_delete)
        val colorButton : ImageButton = view.findViewById(R.id.btn_color)
        val moveButton : ImageButton = view.findViewById(R.id.btn_move)
        val dragButton : ImageButton = view.findViewById(R.id.btn_drag)

        val greenColorPickerButton : ImageButton = view.findViewById(R.id.green_color_picker)
        val blueColorPickerButton : ImageButton = view.findViewById(R.id.blue_color_picker)
        val yellowColorPickerButton : ImageButton = view.findViewById(R.id.yellow_color_picker)
        val pinkColorPickerButton : ImageButton = view.findViewById(R.id.pink_color_picker)
        val colorPaletteBackground : ImageView = view.findViewById(R.id.color_palette_background)

        val deleteDialog : MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(context!!)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(R.string.delete_dialog_text)
                .setNegativeButton(R.string.delete_dialog_no_button_label)
                { _, _ -> }

        val colorPaletteViews : List<View> = listOf(colorPaletteBackground,
                greenColorPickerButton,
                blueColorPickerButton,
                yellowColorPickerButton,
                pinkColorPickerButton)

        fun setColorPaletteVisibility(visibilityStasus : Int, colorPaletteViewList : List<View> = colorPaletteViews){
            for(view in colorPaletteViewList)
                view.visibility = visibilityStasus
        }

        //Bind data from task to the task view
        fun bindTaskDataToView(task: Task) {
            //Assign given task to this viewholder
            this.task = task

            //Assign corresponding drawable as background for task card
            val taskCardDrawableResId = when (task.color) {
                TaskColor.BLUE -> R.drawable.task_card_blue
                TaskColor.YELLOW -> R.drawable.task_card_yellow
                TaskColor.GREEN -> R.drawable.task_card_green
                TaskColor.PINK -> R.drawable.task_card_pink
            }

            //Set text to edittext and set card background
            taskEditText.setText(task.taskText)
            taskLayout.background = ResourcesCompat.getDrawable(resources, taskCardDrawableResId, null)

        }

        //Prepare the view's listeners, called when the viewholder is created
        fun prepareView() : TaskViewHolder{
            //Set button images and listeners of created viewholder's view (MOVE BUTTON)
            when(tasklistType){
                TASKLIST_TYPE_TODO -> { moveButton.setImageResource(R.drawable.ic_arrow_forward_24px) }
                TASKLIST_TYPE_DOING -> { moveButton.setImageResource(R.drawable.ic_done_24px) }
                TASKLIST_TYPE_DONE -> {
                    moveButton.setImageResource(R.drawable.ic_arrow_forward_24px)
                    moveButton.rotation = 180.0f
                }
                else -> { throw Exception("Unrecognized tasklist type") }
            }

            //Set all color controls to invisible at creation
            setColorPaletteVisibility(View.INVISIBLE)

            //Set listeners for color pickers and color palette button, set palette UI up
            colorButton.setOnClickListener{
                if(colorPaletteIsVisible) {
                    //If there is an open color palette, close the last open palette
                    setColorPaletteVisibility(View.INVISIBLE, visibleColorPaletteViewList)

                    //If the pressed color button is the last task view with colorpalette opened,
                    if(visibleColorPaletteViewList[0] != colorPaletteBackground){
                        setColorPaletteVisibility(View.VISIBLE)

                        visibleColorPaletteViewList = colorPaletteViews
                    }
                    else{
                        colorPaletteIsVisible = false
                    }
                }
                else{
                    colorPaletteIsVisible = true
                    setColorPaletteVisibility(View.VISIBLE)

                    visibleColorPaletteViewList = colorPaletteViews
                }
            }
            taskLayout.setOnClickListener {
                if(colorPaletteIsVisible){
                    setColorPaletteVisibility(View.INVISIBLE, visibleColorPaletteViewList)
                    colorPaletteIsVisible = false
                }
            }

            //Add listener for delete button
            deleteButton.setOnClickListener {
                deleteDialog.setPositiveButton(R.string.delete_dialog_yes_button_label)
                { _, _ ->
                    //Delete task from the view model and notify adapter
                    callbacks?.deleteTaskFromViewModel(tasklistType, adapterPosition)
                }.create().show()
            }

            //Add listener for when edit text is changed, save the input to task at view model
            taskEditText.addTextChangedListener {
                task.taskText = it.toString()
            }

            //Set listeners for color picker buttons
            for (i in 1..4) {
                val colorEnum: TaskColor
                val colorResId: Int

                when (i) {
                    1 -> {
                        colorEnum = TaskColor.GREEN
                        colorResId = R.drawable.task_card_green }
                    2 -> {
                        colorEnum = TaskColor.BLUE
                        colorResId = R.drawable.task_card_blue }
                    3 -> {
                        colorEnum = TaskColor.YELLOW
                        colorResId = R.drawable.task_card_yellow }
                    4 -> {
                        colorEnum = TaskColor.PINK
                        colorResId = R.drawable.task_card_pink }
                    else -> throw Exception()
                }

                //Change background to corresponing color on click.
                colorPaletteViews[i].setOnClickListener {
                    taskLayout.background = ResourcesCompat.getDrawable(resources, colorResId, null)

                    if (colorPaletteIsVisible) {
                        setColorPaletteVisibility(View.INVISIBLE, visibleColorPaletteViewList)
                        colorPaletteIsVisible = false
                    }

                    task.color = colorEnum
                }
            }

            //Set drag button listener
            dragButton.setOnTouchListener { v, event ->

                    if (event.actionMasked == MotionEvent.ACTION_DOWN)
                        itemTouchHelper.startDrag(this)

                    v.performClick()
                    return@setOnTouchListener true

            }

            //Set listener for move button. Delete the task from this list and add it to destination list
            moveButton.setOnClickListener {
                val destinationTaskListType = when(tasklistType){
                    TASKLIST_TYPE_TODO, TASKLIST_TYPE_DONE -> TASKLIST_TYPE_DOING
                    TASKLIST_TYPE_DOING -> TASKLIST_TYPE_DONE
                    else -> throw Exception("Unrecognized tasklist type")
                }

                callbacks?.addTaskToViewModel(task, destinationTaskListType)
                callbacks?.deleteTaskFromViewModel(tasklistType, adapterPosition)
            }

            return this
        }
    }

    private inner class TaskViewAdapter(var taskList : LinkedList<Task>)
        : RecyclerView.Adapter<TaskViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            //Inflate the desired task view
            val view = layoutInflater.inflate(R.layout.task_view, parent, false)

            //Create a viewholder that holds the created view, set listeners and prepare the view
            return TaskViewHolder(view).prepareView()
        }

        //Bind task data to view
        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            val task : Task = taskList[position]
            holder.bindTaskDataToView(task)
        }

        override fun getItemCount(): Int = taskList.size

        //Change a tasks position
        fun moveTaskView(from: Int, to: Int){
            val temp = taskList[from]
            taskList.removeAt(from)
            taskList.add(to, temp)
        }
    }
}

